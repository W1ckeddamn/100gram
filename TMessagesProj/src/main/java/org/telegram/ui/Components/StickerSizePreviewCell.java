package org.telegram.ui.Components;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class StickerSizePreviewCell extends LinearLayout {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint servicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint serviceTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gifPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF windowRect = new RectF();
    private final Path clipPath = new Path();
    private final RectF gifRect = new RectF();
    private final Path gifClipPath = new Path();
    private final Path playPath = new Path();
    private final Path checkPath = new Path();

    private final AvatarDrawable avatarDrawable = new AvatarDrawable();
    private final ImageReceiver stickerReceiver;
    private final ImageReceiver gifReceiver;

    private float currentStickerScale = 1.0f;
    private float currentGifScale = 1.0f;
    private ValueAnimator stickerScaleAnimator;
    private ValueAnimator gifScaleAnimator;

    private final View chatWindowView;
    private final LinearLayout stickerChipsContainer;
    private final LinearLayout gifChipsContainer;
    private TextView stickerValueView;
    private TextView gifValueView;

    public interface OnStickerSizeChangedListener {
        void onSizeChanged(int size);
    }

    public interface OnMediaSizeChangedListener {
        void onSizeChanged(int type, int size);
    }

    private OnStickerSizeChangedListener listener;
    private OnMediaSizeChangedListener mediaListener;

    public void setOnStickerSizeChangedListener(OnStickerSizeChangedListener listener) {
        this.listener = listener;
    }

    public void setOnMediaSizeChangedListener(OnMediaSizeChangedListener listener) {
        this.mediaListener = listener;
    }

    public StickerSizePreviewCell(Context context) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        setWillNotDraw(false);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        currentStickerScale = HundredGramConfig.getStickerScale();
        currentGifScale = HundredGramConfig.getGifScale();

        // Configure paints
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(AndroidUtilities.dp(1));

        servicePaint.setColor(0x44000000);

        serviceTextPaint.setColor(Color.WHITE);
        serviceTextPaint.setTextSize(AndroidUtilities.dp(11.5f));
        serviceTextPaint.setTypeface(AndroidUtilities.bold());
        serviceTextPaint.setTextAlign(Paint.Align.CENTER);

        namePaint.setTextSize(AndroidUtilities.dp(12.5f));
        namePaint.setTypeface(AndroidUtilities.bold());

        timePaint.setColor(Color.WHITE);
        timePaint.setTextSize(AndroidUtilities.dp(10.5f));

        badgePaint.setColor(0x55000000);

        badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextSize(AndroidUtilities.dp(9.5f));
        badgeTextPaint.setTypeface(AndroidUtilities.bold());
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);

        playCirclePaint.setColor(0x44000000);
        playCirclePaint.setStyle(Paint.Style.FILL);

        playIconPaint.setColor(Color.WHITE);
        playIconPaint.setStyle(Paint.Style.FILL);

        checkPaint.setColor(Color.WHITE);
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeWidth(AndroidUtilities.dp(1.3f));
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);

        // Avatar initialization
        avatarDrawable.setInfo(5, "Eva", null);

        // Chat window view with ImageReceiver lifecycle
        chatWindowView = new View(context) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                if (stickerReceiver != null) {
                    stickerReceiver.onAttachedToWindow();
                }
                if (gifReceiver != null) {
                    gifReceiver.onAttachedToWindow();
                }
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                if (stickerReceiver != null) {
                    stickerReceiver.onDetachedFromWindow();
                }
                if (gifReceiver != null) {
                    gifReceiver.onDetachedFromWindow();
                }
            }

            @Override
            protected void onDraw(Canvas canvas) {
                drawChatWindow(canvas);
            }
        };
        chatWindowView.setWillNotDraw(false);

        // Receivers attached to chatWindowView
        stickerReceiver = new ImageReceiver(chatWindowView);
        stickerReceiver.setInvalidateAll(true);

        gifReceiver = new ImageReceiver(chatWindowView);
        gifReceiver.setInvalidateAll(true);
        gifReceiver.setRoundRadius(AndroidUtilities.dp(16));

        loadRecentMedia();

        // 1. Add Chat Simulation Window
        addView(chatWindowView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 270, 0, 14, 10, 14, 4));

        // 2. Control rows container
        LinearLayout controlsLayout = new LinearLayout(context);
        controlsLayout.setOrientation(LinearLayout.VERTICAL);
        controlsLayout.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(14));

        // Row 1: Sticker size selector
        TextView[] stickerValRef = new TextView[1];
        LinearLayout stickerRow = createSelectorRow(
                context,
                R.drawable.msg_sticker,
                LocaleController.getString("StickerSize", R.string.StickerSize),
                stickerValRef
        );
        stickerValueView = stickerValRef[0];
        stickerChipsContainer = new LinearLayout(context);
        stickerChipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView stickerScroll = new HorizontalScrollView(context);
        stickerScroll.setHorizontalScrollBarEnabled(false);
        stickerScroll.addView(stickerChipsContainer, LayoutHelper.createScroll(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        stickerRow.addView(stickerScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));
        controlsLayout.addView(stickerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Row 2: GIF size selector
        TextView[] gifValRef = new TextView[1];
        LinearLayout gifRow = createSelectorRow(
                context,
                R.drawable.msg_gif,
                LocaleController.getString("GifSize", R.string.GifSize),
                gifValRef
        );
        gifValueView = gifValRef[0];
        gifChipsContainer = new LinearLayout(context);
        gifChipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView gifScroll = new HorizontalScrollView(context);
        gifScroll.setHorizontalScrollBarEnabled(false);
        gifScroll.addView(gifChipsContainer, LayoutHelper.createScroll(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        gifRow.addView(gifScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));
        controlsLayout.addView(gifRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));

        addView(controlsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        updateValueLabels();
        rebuildChips();
    }

    private void loadRecentMedia() {
        try {
            int currentAccount = UserConfig.selectedAccount;
            ArrayList<TLRPC.Document> recentStickers = MediaDataController.getInstance(currentAccount).getRecentStickersNoCopy(MediaDataController.TYPE_IMAGE);
            if (recentStickers != null && !recentStickers.isEmpty()) {
                TLRPC.Document doc = recentStickers.get(0);
                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(doc.thumbs, 120);
                stickerReceiver.setImage(ImageLocation.getForDocument(doc), "140_140", thumb != null ? ImageLocation.getForDocument(thumb, doc) : null, null, "webp", null, 0);
            }

            ArrayList<TLRPC.Document> recentGifs = MediaDataController.getInstance(currentAccount).getRecentGifs();
            if (recentGifs != null && !recentGifs.isEmpty()) {
                TLRPC.Document doc = recentGifs.get(0);
                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(doc.thumbs, 200);
                gifReceiver.setImage(ImageLocation.getForDocument(doc), "200_150", thumb != null ? ImageLocation.getForDocument(thumb, doc) : null, null, (String) null, null, 0);
            }
        } catch (Exception e) {
            // Ignore - fallback to built-in graphics
        }
    }

    private LinearLayout createSelectorRow(Context context, int iconRes, String title, TextView[] outValueView) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(iconRes);
        iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN));
        header.addView(iconView, LayoutHelper.createLinear(20, 20, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(13.5f);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTypeface(AndroidUtilities.bold());
        header.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL));

        TextView valueView = new TextView(context);
        valueView.setTextSize(12.5f);
        valueView.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        valueView.setTypeface(AndroidUtilities.bold());
        header.addView(valueView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));
        if (outValueView != null && outValueView.length > 0) {
            outValueView[0] = valueView;
        }

        container.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        return container;
    }

    private void updateValueLabels() {
        if (stickerValueView != null) {
            stickerValueView.setText(getSizeLabel(HundredGramConfig.stickerSize));
        }
        if (gifValueView != null) {
            gifValueView.setText(getSizeLabel(HundredGramConfig.gifSize));
        }
    }

    private String getSizeLabel(int size) {
        switch (size) {
            case HundredGramConfig.MEDIA_SIZE_MINIMAL:
                return LocaleController.getString("MediaSizeMinimal", R.string.MediaSizeMinimal) + " • 50%";
            case HundredGramConfig.MEDIA_SIZE_COMPACT:
                return LocaleController.getString("MediaSizeCompact", R.string.MediaSizeCompact) + " • 75%";
            case HundredGramConfig.MEDIA_SIZE_LARGE:
                return LocaleController.getString("MediaSizeLarge", R.string.MediaSizeLarge) + " • 125%";
            case HundredGramConfig.MEDIA_SIZE_HUGE:
                return LocaleController.getString("MediaSizeHuge", R.string.MediaSizeHuge) + " • 150%";
            case HundredGramConfig.MEDIA_SIZE_DEFAULT:
            default:
                return LocaleController.getString("MediaSizeDefault", R.string.MediaSizeDefault) + " • 100%";
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
    }

    public void update() {
        currentStickerScale = HundredGramConfig.getStickerScale();
        currentGifScale = HundredGramConfig.getGifScale();
        updateValueLabels();
        rebuildChips();
        if (chatWindowView != null) {
            chatWindowView.invalidate();
        }
    }

    private void drawChatWindow(Canvas canvas) {
        int width = chatWindowView.getWidth();
        int height = chatWindowView.getHeight();
        if (width <= 0 || height <= 0) return;

        boolean isDark = Theme.isCurrentThemeDark();

        // Clip chat window to rounded rectangle (16dp)
        float cornerRadius = AndroidUtilities.dp(16);
        windowRect.set(0, 0, width, height);
        clipPath.reset();
        clipPath.addRoundRect(windowRect, cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clipPath);

        // 1. Draw Chat Wallpaper / Themed Background
        Drawable wallpaper = Theme.getCachedWallpaper();
        if (wallpaper != null) {
            int w = wallpaper.getIntrinsicWidth();
            int h = wallpaper.getIntrinsicHeight();
            if (w <= 0 || h <= 0) {
                wallpaper.setBounds(0, 0, width, height);
            } else {
                float scale = Math.max((float) width / w, (float) height / h);
                int dw = (int) (w * scale);
                int dh = (int) (h * scale);
                int dx = (width - dw) / 2;
                int dy = (height - dh) / 2;
                wallpaper.setBounds(dx, dy, dx + dw, dy + dh);
            }
            wallpaper.draw(canvas);
        } else {
            int topColor = isDark ? 0xFF141F2D : 0xFFCFDEF3;
            int bottomColor = isDark ? 0xFF0D141C : 0xFFE0EAFC;
            bgPaint.setShader(new LinearGradient(0, 0, 0, height, topColor, bottomColor, Shader.TileMode.CLAMP));
            canvas.drawRect(windowRect, bgPaint);
            bgPaint.setShader(null);
        }

        // Outer window border
        borderPaint.setColor(isDark ? 0x22FFFFFF : 0x1A000000);
        canvas.drawRoundRect(windowRect, cornerRadius, cornerRadius, borderPaint);

        // 2. Chat Date Service Bubble (Top Center)
        String dateText = LocaleController.formatDateChat(System.currentTimeMillis() / 1000L);
        float dateTextWidth = serviceTextPaint.measureText(dateText);
        float dateBubbleW = dateTextWidth + AndroidUtilities.dp(20);
        float dateBubbleH = AndroidUtilities.dp(20);
        float dateBubbleX = (width - dateBubbleW) / 2f;
        float dateBubbleY = AndroidUtilities.dp(10);
        RectF dateRect = AndroidUtilities.rectTmp;
        dateRect.set(dateBubbleX, dateBubbleY, dateBubbleX + dateBubbleW, dateBubbleY + dateBubbleH);
        servicePaint.setColor(0x44000000);
        canvas.drawRoundRect(dateRect, dateBubbleH / 2f, dateBubbleH / 2f, servicePaint);
        canvas.drawText(dateText, width / 2f, dateBubbleY + AndroidUtilities.dp(14), serviceTextPaint);

        // 3. Incoming Message: Real Telegram Sticker
        float avatarLeft = AndroidUtilities.dp(12);
        float avatarTop = AndroidUtilities.dp(36);
        float avatarSize = AndroidUtilities.dp(32);
        avatarDrawable.setBounds((int) avatarLeft, (int) avatarTop, (int) (avatarLeft + avatarSize), (int) (avatarTop + avatarSize));
        avatarDrawable.draw(canvas);

        // Contact name above sticker
        float nameLeft = avatarLeft + avatarSize + AndroidUtilities.dp(10);
        float nameY = avatarTop + AndroidUtilities.dp(13);
        namePaint.setColor(Theme.getColor(Theme.keys_avatar_nameInMessage[4]));
        canvas.drawText("Eva", nameLeft, nameY, namePaint);

        // The sticker itself (floating naturally on wallpaper with no card background)
        float stickerLeft = nameLeft;
        float stickerTop = avatarTop + AndroidUtilities.dp(20);
        float baseStickerSize = Math.min(width * 0.40f, AndroidUtilities.dp(115));
        float stickerSize = baseStickerSize * currentStickerScale;

        if (stickerReceiver != null && stickerReceiver.hasBitmapImage()) {
            stickerReceiver.setImageCoords(stickerLeft, stickerTop, stickerSize, stickerSize);
            stickerReceiver.draw(canvas);
        } else {
            // Elegant fallback mascot
            Drawable planeDrawable = ContextCompat.getDrawable(getContext(), R.drawable.intro_tg_plane);
            if (planeDrawable != null) {
                planeDrawable.setBounds((int) stickerLeft, (int) stickerTop, (int) (stickerLeft + stickerSize), (int) (stickerTop + stickerSize));
                planeDrawable.draw(canvas);
            }
        }

        // Sticker time capsule pill (floating at bottom-right corner of sticker)
        String stickerTime = "11:58";
        float stickerTimeW = timePaint.measureText(stickerTime);
        float sPillW = stickerTimeW + AndroidUtilities.dp(10);
        float sPillH = AndroidUtilities.dp(15);
        float sPillX = stickerLeft + stickerSize - sPillW;
        float sPillY = stickerTop + stickerSize - sPillH;
        RectF sPillRect = new RectF(sPillX, sPillY, sPillX + sPillW, sPillY + sPillH);
        badgePaint.setColor(0x55000000);
        canvas.drawRoundRect(sPillRect, sPillH / 2f, sPillH / 2f, badgePaint);
        canvas.drawText(stickerTime, sPillX + AndroidUtilities.dp(5), sPillY + AndroidUtilities.dp(11), timePaint);

        // 4. Outgoing Message: Real Telegram GIF Video Card (Right Aligned)
        float baseGifW = Math.min(width * 0.48f, AndroidUtilities.dp(150));
        float baseGifH = baseGifW * 0.70f;
        float gifW = baseGifW * currentGifScale;
        float gifH = baseGifH * currentGifScale;
        float gifRight = width - AndroidUtilities.dp(12);
        float gifLeft = gifRight - gifW;
        float gifTop = height - gifH - AndroidUtilities.dp(12);
        gifRect.set(gifLeft, gifTop, gifRight, gifTop + gifH);

        float gifRadius = AndroidUtilities.dp(16);

        canvas.save();
        gifClipPath.reset();
        gifClipPath.addRoundRect(gifRect, gifRadius, gifRadius, Path.Direction.CW);
        canvas.clipPath(gifClipPath);

        if (gifReceiver != null && gifReceiver.hasBitmapImage()) {
            gifReceiver.setImageCoords(gifLeft, gifTop, gifW, gifH);
            gifReceiver.draw(canvas);
        } else {
            // Realistic cinematic video scene
            int gifTopColor = isDark ? 0xFF1C2D3D : 0xFF2A5298;
            int gifBottomColor = isDark ? 0xFF111E29 : 0xFF1E3C72;
            gifPaint.setShader(new LinearGradient(gifLeft, gifTop, gifRight, gifTop + gifH, gifTopColor, gifBottomColor, Shader.TileMode.CLAMP));
            canvas.drawRect(gifRect, gifPaint);
            gifPaint.setShader(null);
        }

        // Center Play glyph (translucent circular button)
        float playCenterX = gifLeft + gifW / 2f;
        float playCenterY = gifTop + gifH / 2f;
        float playRadius = AndroidUtilities.dp(16) * Math.min(1.2f, Math.max(0.75f, currentGifScale));
        playCirclePaint.setColor(0x4D000000);
        canvas.drawCircle(playCenterX, playCenterY, playRadius, playCirclePaint);

        playPath.reset();
        float triSize = playRadius * 0.46f;
        playPath.moveTo(playCenterX - triSize * 0.6f, playCenterY - triSize);
        playPath.lineTo(playCenterX + triSize, playCenterY);
        playPath.lineTo(playCenterX - triSize * 0.6f, playCenterY + triSize);
        playPath.close();
        playIconPaint.setColor(Color.WHITE);
        canvas.drawPath(playPath, playIconPaint);

        // Top-left "GIF" badge pill
        float gifBadgeW = AndroidUtilities.dp(25);
        float gifBadgeH = AndroidUtilities.dp(15);
        float gifBadgeX = gifLeft + AndroidUtilities.dp(6);
        float gifBadgeY = gifTop + AndroidUtilities.dp(6);
        RectF gifBadgeRect = new RectF(gifBadgeX, gifBadgeY, gifBadgeX + gifBadgeW, gifBadgeY + gifBadgeH);
        badgePaint.setColor(0x55000000);
        canvas.drawRoundRect(gifBadgeRect, gifBadgeH / 2f, gifBadgeH / 2f, badgePaint);
        canvas.drawText("GIF", gifBadgeX + gifBadgeW / 2f, gifBadgeY + AndroidUtilities.dp(11), badgeTextPaint);

        // Bottom-right outgoing timestamp with double checkmarks ("12:00 ✓✓")
        String gifTime = "12:00";
        float gifTimeW = timePaint.measureText(gifTime);
        float checkW = AndroidUtilities.dp(14);
        float gPillW = gifTimeW + checkW + AndroidUtilities.dp(9);
        float gPillH = AndroidUtilities.dp(15);
        float gPillX = gifRight - gPillW - AndroidUtilities.dp(6);
        float gPillY = gifTop + gifH - gPillH - AndroidUtilities.dp(6);
        RectF gPillRect = new RectF(gPillX, gPillY, gPillX + gPillW, gPillY + gPillH);
        canvas.drawRoundRect(gPillRect, gPillH / 2f, gPillH / 2f, badgePaint);
        canvas.drawText(gifTime, gPillX + AndroidUtilities.dp(5), gPillY + AndroidUtilities.dp(11), timePaint);

        // Double checkmarks
        float cx = gPillX + gifTimeW + AndroidUtilities.dp(7);
        float cy = gPillY + AndroidUtilities.dp(8);

        checkPath.reset();
        checkPath.moveTo(cx, cy);
        checkPath.lineTo(cx + AndroidUtilities.dp(2.5f), cy + AndroidUtilities.dp(2.5f));
        checkPath.lineTo(cx + AndroidUtilities.dp(6.5f), cy - AndroidUtilities.dp(2.5f));
        canvas.drawPath(checkPath, checkPaint);

        checkPath.reset();
        checkPath.moveTo(cx + AndroidUtilities.dp(3.5f), cy);
        checkPath.lineTo(cx + AndroidUtilities.dp(6), cy + AndroidUtilities.dp(2.5f));
        checkPath.lineTo(cx + AndroidUtilities.dp(10), cy - AndroidUtilities.dp(2.5f));
        canvas.drawPath(checkPath, checkPaint);

        canvas.restore(); // Restore GIF clipping

        canvas.restore(); // Restore Window clipping
    }

    private void animateToStickerScale(float targetScale) {
        if (stickerScaleAnimator != null) {
            stickerScaleAnimator.cancel();
        }
        stickerScaleAnimator = ValueAnimator.ofFloat(currentStickerScale, targetScale);
        stickerScaleAnimator.setDuration(320);
        stickerScaleAnimator.setInterpolator(new OvershootInterpolator(1.2f));
        stickerScaleAnimator.addUpdateListener(animation -> {
            currentStickerScale = (float) animation.getAnimatedValue();
            chatWindowView.invalidate();
        });
        stickerScaleAnimator.start();
    }

    private void animateToGifScale(float targetScale) {
        if (gifScaleAnimator != null) {
            gifScaleAnimator.cancel();
        }
        gifScaleAnimator = ValueAnimator.ofFloat(currentGifScale, targetScale);
        gifScaleAnimator.setDuration(320);
        gifScaleAnimator.setInterpolator(new OvershootInterpolator(1.2f));
        gifScaleAnimator.addUpdateListener(animation -> {
            currentGifScale = (float) animation.getAnimatedValue();
            chatWindowView.invalidate();
        });
        gifScaleAnimator.start();
    }

    private void rebuildChips() {
        buildStickerChips();
        buildGifChips();
    }

    private void buildStickerChips() {
        stickerChipsContainer.removeAllViews();
        int current = HundredGramConfig.stickerSize;

        int[] sizes = {
                HundredGramConfig.MEDIA_SIZE_MINIMAL,
                HundredGramConfig.MEDIA_SIZE_COMPACT,
                HundredGramConfig.MEDIA_SIZE_DEFAULT,
                HundredGramConfig.MEDIA_SIZE_LARGE,
                HundredGramConfig.MEDIA_SIZE_HUGE
        };
        String[] labels = { "50%", "75%", "100%", "125%", "150%" };

        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            boolean selected = (size == current);
            TextView chip = createChip(labels[i], selected);
            chip.setOnClickListener(v -> {
                HundredGramConfig.setStickerSize(size);
                updateValueLabels();
                rebuildChips();
                animateToStickerScale(HundredGramConfig.getStickerScale());
                if (listener != null) {
                    listener.onSizeChanged(size);
                }
                if (mediaListener != null) {
                    mediaListener.onSizeChanged(0, size);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(
                        NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL
                );
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.rightMargin = AndroidUtilities.dp(6);
            stickerChipsContainer.addView(chip, lp);
        }
    }

    private void buildGifChips() {
        gifChipsContainer.removeAllViews();
        int current = HundredGramConfig.gifSize;

        int[] sizes = {
                HundredGramConfig.MEDIA_SIZE_MINIMAL,
                HundredGramConfig.MEDIA_SIZE_COMPACT,
                HundredGramConfig.MEDIA_SIZE_DEFAULT,
                HundredGramConfig.MEDIA_SIZE_LARGE,
                HundredGramConfig.MEDIA_SIZE_HUGE
        };
        String[] labels = { "50%", "75%", "100%", "125%", "150%" };

        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            boolean selected = (size == current);
            TextView chip = createChip(labels[i], selected);
            chip.setOnClickListener(v -> {
                HundredGramConfig.setGifSize(size);
                updateValueLabels();
                rebuildChips();
                animateToGifScale(HundredGramConfig.getGifScale());
                if (mediaListener != null) {
                    mediaListener.onSizeChanged(1, size);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(
                        NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL
                );
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.rightMargin = AndroidUtilities.dp(6);
            gifChipsContainer.addView(chip, lp);
        }
    }

    private TextView createChip(String text, boolean selected) {
        TextView chip = new TextView(getContext());
        chip.setText(text);
        chip.setTextSize(13f);
        chip.setGravity(Gravity.CENTER);
        chip.setTypeface(selected ? AndroidUtilities.bold() : null);
        chip.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(7), AndroidUtilities.dp(14), AndroidUtilities.dp(7));

        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        if (selected) {
            chip.setTextColor(Color.WHITE);
            chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), accent));
        } else {
            chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            chip.setBackground(Theme.createRoundRectDrawable(
                    AndroidUtilities.dp(14),
                    Theme.isCurrentThemeDark() ? 0x1EFFFFFF : 0x14000000
            ));
        }
        return chip;
    }
}
