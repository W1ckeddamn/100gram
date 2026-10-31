package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramLyricsHelper;
import org.telegram.messenger.HundredGramUtils;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

/**
 * BottomSheet для отображения синхронизированных (LRC) или обычных текстов песен в аудиоплеере 100gram.
 */
public class HundredGramLyricsBottomSheet extends BottomSheet implements NotificationCenter.NotificationCenterDelegate {

    private final int currentAccount;
    private final MessageObject messageObject;
    private HundredGramLyricsHelper.LyricsResult lyricsResult;

    private FrameLayout containerView;
    private RadialProgressView progressView;
    private TextView emptyTextView;
    private ScrollView plainScrollView;
    private TextView plainTextView;
    private RecyclerListView syncedListView;
    private LinearLayoutManager layoutManager;
    private LyricsAdapter lyricsAdapter;

    private int activeLineIndex = -1;
    private boolean isUserScrolling = false;
    private long lastUserScrollTime = 0;

    public HundredGramLyricsBottomSheet(Context context, int account, MessageObject messageObject, Theme.ResourcesProvider resourcesProvider) {
        super(context, true, resourcesProvider);
        this.currentAccount = account;
        this.messageObject = messageObject;

        setApplyBottomPadding(false);
        fixNavigationBar(getThemedColor(Theme.key_dialogBackground));

        initViews(context);
        loadLyrics();

        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
    }

    private void initViews(Context context) {
        containerView = new FrameLayout(context);
        containerView.setBackgroundColor(getThemedColor(Theme.key_dialogBackground));

        // Drag handle bar
        View handleView = new View(context);
        handleView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2), getThemedColor(Theme.key_sheet_scrollUp)));
        containerView.addView(handleView, LayoutHelper.createFrame(36, 4, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 8, 0, 0));

        // Top Header
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(messageObject != null ? messageObject.getMusicTitle() : LocaleController.getString("LyricsInPlayer", R.string.LyricsInPlayer));
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView artistView = new TextView(context);
        artistView.setText(messageObject != null ? messageObject.getMusicAuthor() : "");
        artistView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        artistView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        artistView.setSingleLine(true);
        artistView.setEllipsize(TextUtils.TruncateAt.END);
        textLayout.addView(artistView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        headerLayout.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        // Copy button
        ImageView copyButton = new ImageView(context);
        copyButton.setImageResource(R.drawable.msg_copy);
        copyButton.setScaleType(ImageView.ScaleType.CENTER);
        copyButton.setColorFilter(getThemedColor(Theme.key_dialogIcon));
        copyButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
        copyButton.setOnClickListener(v -> copyLyrics());
        headerLayout.addView(copyButton, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL, 0, 0, 4, 0));

        // Close button
        ImageView closeButton = new ImageView(context);
        closeButton.setImageResource(R.drawable.msg_close);
        closeButton.setScaleType(ImageView.ScaleType.CENTER);
        closeButton.setColorFilter(getThemedColor(Theme.key_dialogIcon));
        closeButton.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(18)));
        closeButton.setOnClickListener(v -> dismiss());
        headerLayout.addView(closeButton, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL));

        containerView.addView(headerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // Content Frame
        FrameLayout contentArea = new FrameLayout(context);
        containerView.addView(contentArea, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 460, Gravity.TOP, 0, 68, 0, 0));

        // Loading view
        progressView = new RadialProgressView(context, resourcesProvider);
        progressView.setSize(AndroidUtilities.dp(36));
        contentArea.addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        // Empty view
        emptyTextView = new TextView(context);
        emptyTextView.setText(LocaleController.getString("LyricsNotFound", R.string.LyricsNotFound));
        emptyTextView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        emptyTextView.setGravity(Gravity.CENTER);
        emptyTextView.setVisibility(View.GONE);
        contentArea.addView(emptyTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 24, 0, 24, 0));

        // Plain lyrics scrollview
        plainScrollView = new ScrollView(context);
        plainScrollView.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(24));
        plainScrollView.setClipToPadding(false);
        plainTextView = new TextView(context);
        plainTextView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        plainTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        plainTextView.setLineSpacing(AndroidUtilities.dp(6), 1.2f);
        plainTextView.setTextIsSelectable(true);
        plainScrollView.addView(plainTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        plainScrollView.setVisibility(View.GONE);
        contentArea.addView(plainScrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Synced lyrics recycler view
        syncedListView = new RecyclerListView(context);
        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        syncedListView.setLayoutManager(layoutManager);
        syncedListView.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(80), AndroidUtilities.dp(20), AndroidUtilities.dp(160));
        syncedListView.setClipToPadding(false);
        syncedListView.setVerticalScrollBarEnabled(false);
        lyricsAdapter = new LyricsAdapter();
        syncedListView.setAdapter(lyricsAdapter);
        syncedListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isUserScrolling = true;
                    lastUserScrollTime = System.currentTimeMillis();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    AndroidUtilities.runOnUIThread(() -> isUserScrolling = false, 3000);
                }
            }
        });
        syncedListView.setOnItemClickListener((view, position) -> {
            if (lyricsResult != null && lyricsResult.isSynced && position >= 0 && position < lyricsResult.lines.size()) {
                HundredGramLyricsHelper.LyricsLine line = lyricsResult.lines.get(position);
                int duration = messageObject != null ? (int) messageObject.getDuration() : 0;
                if (duration > 0) {
                    float prog = (float) line.timeMs / (duration * 1000f);
                    MediaController.getInstance().seekToProgress(messageObject, Math.max(0f, Math.min(1f, prog)));
                    activeLineIndex = position;
                    lyricsAdapter.notifyDataSetChanged();
                }
            }
        });
        syncedListView.setVisibility(View.GONE);
        contentArea.addView(syncedListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        setCustomView(containerView);
    }

    private void loadLyrics() {
        if (messageObject == null) {
            showEmpty();
            return;
        }

        HundredGramLyricsHelper.fetchLyrics(currentAccount, messageObject, result -> {
            progressView.setVisibility(View.GONE);
            lyricsResult = result;
            if (result == null || !result.hasLyrics()) {
                showEmpty();
                return;
            }

            if (result.isSynced && !result.lines.isEmpty()) {
                syncedListView.setVisibility(View.VISIBLE);
                plainScrollView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
                lyricsAdapter.notifyDataSetChanged();
                updateActiveLine();
            } else {
                syncedListView.setVisibility(View.GONE);
                plainScrollView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.GONE);
                plainTextView.setText(result.getFullReadableText());
            }
        });
    }

    private void showEmpty() {
        progressView.setVisibility(View.GONE);
        plainScrollView.setVisibility(View.GONE);
        syncedListView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
    }

    private void copyLyrics() {
        if (lyricsResult == null || !lyricsResult.hasLyrics()) return;
        String text = lyricsResult.getFullReadableText();
        if (!TextUtils.isEmpty(text)) {
            AndroidUtilities.addToClipboard(text);
            BulletinFactory.of(container, resourcesProvider).createSimpleBulletin(R.drawable.msg_copy, LocaleController.getString("LyricsCopied", R.string.LyricsCopied)).show();
        }
    }

    private void updateActiveLine() {
        if (lyricsResult == null || !lyricsResult.isSynced || messageObject == null) return;
        MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
        if (playing == null || playing.getId() != messageObject.getId()) return;

        int duration = (int) messageObject.getDuration();
        if (duration <= 0) return;

        long currentPosMs = (long) (playing.audioProgress * duration * 1000f);
        int newIndex = lyricsResult.findCurrentLineIndex(currentPosMs);

        if (newIndex != activeLineIndex) {
            activeLineIndex = newIndex;
            lyricsAdapter.notifyDataSetChanged();

            if (!isUserScrolling && System.currentTimeMillis() - lastUserScrollTime > 3000) {
                if (activeLineIndex >= 0 && activeLineIndex < lyricsResult.lines.size()) {
                    layoutManager.scrollToPositionWithOffset(activeLineIndex, AndroidUtilities.dp(90));
                }
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingProgressDidChanged || id == NotificationCenter.messagePlayingPlayStateChanged) {
            if (lyricsResult != null && lyricsResult.isSynced) {
                updateActiveLine();
            }
        }
    }

    @Override
    public void dismiss() {
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        super.dismiss();
    }

    private class LyricsAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10));
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            return new RecyclerListView.Holder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextView textView = (TextView) holder.itemView;
            if (lyricsResult == null || position < 0 || position >= lyricsResult.lines.size()) {
                textView.setText("");
                return;
            }

            HundredGramLyricsHelper.LyricsLine line = lyricsResult.lines.get(position);
            textView.setText(line.text);

            boolean isActive = (position == activeLineIndex);
            if (isActive) {
                textView.setTextColor(getThemedColor(Theme.key_dialogTextBlue2));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                textView.setTypeface(AndroidUtilities.bold());
                textView.setAlpha(1.0f);
            } else {
                textView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                textView.setTypeface(Typeface.DEFAULT);
                textView.setAlpha(0.45f);
            }
        }

        @Override
        public int getItemCount() {
            return (lyricsResult != null && lyricsResult.isSynced) ? lyricsResult.lines.size() : 0;
        }
    }
}
