package com.exteragram.messenger.nowplaying.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;

import com.exteragram.messenger.api.dto.NowPlayingDTO;
import com.exteragram.messenger.nowplaying.ServiceEmoji;

import java.util.List;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.utils.ViewOutlineProviderImpl;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PlayPauseDrawable;
import org.telegram.ui.Components.ScaleStateListAnimator;

public abstract class NowPlayingCard extends FrameLayout {

    private static final float[] nowPlayingPattern = {
            -5.5f, 20.0f, 20.0f, 0.35f,
            -5.5f, -20.0f, 20.0f, 0.35f,
            -36.0f, -42.0f, 22.0f, 0.375f,
            -36.0f, 0.0f, 25.0f, 0.425f,
            -36.0f, 42.0f, 22.0f, 0.375f,
            -70.0f, 22.0f, 23.0f, 0.35f,
            -70.0f, -22.0f, 23.0f, 0.35f,
            -99.0f, 46.0f, 21.0f, 0.275f,
            -99.0f, 0.0f, 22.0f, 0.325f,
            -99.0f, -46.0f, 21.0f, 0.275f,
            -128.0f, -23.0f, 20.0f, 0.225f,
            -128.0f, 23.0f, 20.0f, 0.225f
    };

    private static void drawNowPlayingPattern(Canvas canvas, Drawable pattern, float w, float h, float alpha) {
        if (alpha <= 0.0f || pattern == null) {
            return;
        }
        int i = 0;
        while (i < nowPlayingPattern.length) {
            float f = nowPlayingPattern[i];
            float f2 = nowPlayingPattern[i + 1];
            float f3 = nowPlayingPattern[i + 2];
            float f4 = nowPlayingPattern[i + 3];
            float f5 = h / 2.0f;
            pattern.setBounds(
                    (int) ((AndroidUtilities.dpf2(f) + w) - (AndroidUtilities.dpf2(f3) / 2.0f)),
                    (int) ((AndroidUtilities.dpf2(f2) + f5) - (AndroidUtilities.dpf2(f3) / 2.0f)),
                    (int) (AndroidUtilities.dpf2(f) + w + (AndroidUtilities.dpf2(f3) / 2.0f)),
                    (int) (f5 + AndroidUtilities.dpf2(f2) + (AndroidUtilities.dpf2(f3) / 2.0f))
            );
            pattern.setAlpha((int) (255.0f * alpha * f4));
            pattern.draw(canvas);
            i += 4;
        }
    }

    private final SimpleTextView albumView;
    private final SimpleTextView artistView;
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private AudioFocusRequest audioFocusRequest;
    private final AudioManager audioManager;
    private final FrameLayout cardLayout;
    private long currentDocId = -1L;
    private String currentPreviewUrl;
    private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable emoji;
    private final BackupImageView imageView;
    private boolean isPlaying;
    private final SimpleTextView nameView;
    private NowPlayingCardData nowPlayingCardData;
    private final ImageView playPauseButton;
    private PlayPauseDrawable playPauseDrawable;
    private ExoPlayer player;
    private final Theme.ResourcesProvider resourcesProvider;
    private boolean resumeOnFocusGain;

    public abstract void onSavedMusicClick();

    public NowPlayingCard(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.audioFocusChangeListener = this::onAudioFocusChanged;
        setClickable(false);
        setWillNotDraw(false);

        FrameLayout frameLayout = new FrameLayout(context) {
            @Override
            public void dispatchDraw(Canvas canvas) {
                if (nowPlayingCardData != null && emoji != null) {
                    Integer accentColor = nowPlayingCardData.getAccentColor();
                    if (accentColor == null) {
                        accentColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText);
                    }
                    emoji.setColor(accentColor);
                    float width = getWidth();
                    float height = getHeight();
                    float alpha = nowPlayingCardData.getCoverBitmap() == null ? 0.4f : 1.0f;
                    drawNowPlayingPattern(canvas, emoji, width, height, alpha);
                }
                super.dispatchDraw(canvas);
            }
        };

        GradientDrawable gradientDrawable = new GradientDrawable() {
            @Override
            public void onBoundsChange(Rect r) {
                super.onBoundsChange(r);
                setGradientRadius(r.width() * 2.0f);
            }
        };
        gradientDrawable.setCornerRadius(AndroidUtilities.dp(12));
        frameLayout.setBackground(gradientDrawable);
        frameLayout.setClipToOutline(true);
        frameLayout.setOutlineProvider(ViewOutlineProviderImpl.fromDrawable(gradientDrawable));
        frameLayout.setClickable(true);
        ScaleStateListAnimator.apply(frameLayout, 0.035f, 1.5f);
        this.cardLayout = frameLayout;
        addView(frameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        this.emoji = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(frameLayout, false, AndroidUtilities.dp(20.0f), 13);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        frameLayout.addView(linearLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 119, 12, 12, 12, 12));

        BackupImageView backupImageView = new BackupImageView(context);
        backupImageView.setRoundRadius((int) getCoverCornerRadius());
        this.imageView = backupImageView;
        linearLayout.addView(backupImageView, LayoutHelper.createLinear(64, 64, 19, 0, 0, 12, 0));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 16));

        SimpleTextView simpleTextView = new SimpleTextView(context);
        simpleTextView.setGravity(19);
        simpleTextView.setTextColor(-1);
        simpleTextView.setTextSize(16);
        simpleTextView.setTypeface(AndroidUtilities.bold());
        NotificationCenter.listenEmojiLoading(simpleTextView);
        this.nameView = simpleTextView;
        textLayout.addView(simpleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 22));

        SimpleTextView simpleTextView2 = new SimpleTextView(context);
        simpleTextView2.setGravity(19);
        simpleTextView2.setTextSize(14);
        simpleTextView2.setTextColor(-1);
        simpleTextView2.setAlpha(0.6f);
        NotificationCenter.listenEmojiLoading(simpleTextView2);
        this.artistView = simpleTextView2;
        textLayout.addView(simpleTextView2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 19, 0.0f, 2.0f, 0.0f, 0.0f));

        SimpleTextView simpleTextView3 = new SimpleTextView(context);
        simpleTextView3.setGravity(19);
        simpleTextView3.setTextSize(14);
        simpleTextView3.setTextColor(-1);
        simpleTextView3.setAlpha(0.6f);
        NotificationCenter.listenEmojiLoading(simpleTextView3);
        this.albumView = simpleTextView3;
        textLayout.addView(simpleTextView3, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 19, 0.0f, 2.0f, 0.0f, 0.0f));

        PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable(16);
        playPauseDrawable.setPause(false);
        playPauseDrawable.setColor(-1);
        this.playPauseDrawable = playPauseDrawable;

        ImageView playButton = new ImageView(context);
        ScaleStateListAnimator.apply(playButton);
        playButton.setScaleType(ImageView.ScaleType.CENTER);
        playButton.setImageDrawable(this.playPauseDrawable);
        playButton.setOnClickListener(v -> togglePlayPause());
        this.playPauseButton = playButton;
        linearLayout.addView(playButton, LayoutHelper.createLinear(32, 32, 16, 8, 0, 8, 0));
    }

    private void onAudioFocusChanged(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            if (player != null && player.isPlaying()) {
                resumeOnFocusGain = true;
                player.pause();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            resumeOnFocusGain = false;
            if (player != null) {
                player.pause();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN && resumeOnFocusGain) {
            if (player != null) {
                player.play();
            }
            resumeOnFocusGain = false;
        }
    }

    private float getCoverCornerRadius() {
        return AndroidUtilities.dp(8);
    }

    public void set(final NowPlayingCardData cardData) {
        this.nowPlayingCardData = cardData;
        final NowPlayingDTO nowPlayingDTO = cardData.getNowPlayingDTO();
        List<String> artists = nowPlayingDTO.getArtists();
        this.artistView.setText(null);
        this.nameView.setText(null);
        this.albumView.setText(null);

        if (artists == null || artists.isEmpty()) {
            this.artistView.setText(LocaleController.getString(R.string.AudioUnknownArtist));
        } else {
            this.artistView.setText(TextUtils.join(", ", artists));
        }
        this.nameView.setText(Emoji.replaceEmoji(nowPlayingDTO.getTrackName(), this.nameView.getPaint().getFontMetricsInt(), false));

        String albumName = nowPlayingDTO.getAlbumName();
        boolean hasAlbum = !TextUtils.isEmpty(albumName) && !TextUtils.equals(nowPlayingDTO.getTrackName(), albumName);
        this.albumView.setVisibility(hasAlbum ? View.VISIBLE : View.GONE);
        if (hasAlbum) {
            this.albumView.setText(Emoji.replaceEmoji(albumName, this.albumView.getPaint().getFontMetricsInt(), false));
        }

        setPadding(AndroidUtilities.dp(12.0f), 0, AndroidUtilities.dp(12.0f), 0);
        Drawable background = this.cardLayout.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.mutate();
            gradientDrawable.setDither(true);
            gradientDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            gradientDrawable.setGradientCenter(1.0f, 0.5f);
            Integer backgroundColor = cardData.getBackgroundColor();
            int intValue = backgroundColor != null ? backgroundColor : getThemedColor(Theme.key_windowBackgroundWhite);
            gradientDrawable.setColors(new int[]{intValue, intValue});
        }

        String previewUrl = nowPlayingDTO.getPreviewUrl();
        boolean hasPreview = !TextUtils.isEmpty(previewUrl) && !"TELEGRAM".equals(nowPlayingDTO.getPlatform());
        this.playPauseButton.setVisibility(hasPreview ? View.VISIBLE : View.GONE);

        int dp = AndroidUtilities.dp(32.0f);
        Integer accentColor = cardData.getAccentColor();
        this.playPauseButton.setBackground(Theme.createCircleDrawable(dp, accentColor != null ? accentColor : getThemedColor(Theme.key_featuredStickers_addButton)));

        long documentId = (cardData.getUserEmoji() <= 0 || !"TELEGRAM".equals(nowPlayingDTO.getPlatform()))
                ? ServiceEmoji.fromString(nowPlayingDTO.getPlatform()).getDocumentId()
                : cardData.getUserEmoji();

        if (documentId != this.currentDocId) {
            this.currentDocId = documentId;
            this.emoji.set(documentId, true);
        }

        this.cardLayout.setOnClickListener(v -> {
            if ("TELEGRAM".equals(nowPlayingDTO.getPlatform())) {
                onSavedMusicClick();
                return;
            }
            String songUrl = cardData.getNowPlayingDTO().getSongUrl();
            if (!TextUtils.isEmpty(songUrl)) {
                Browser.openUrl(getContext(), songUrl);
            }
        });

        this.cardLayout.setOnLongClickListener(v -> {
            if ("TELEGRAM".equals(nowPlayingDTO.getPlatform())) {
                onSavedMusicClick();
                return true;
            }
            String songUrl = cardData.getNowPlayingDTO().getSongUrl();
            if (!TextUtils.isEmpty(songUrl)) {
                Browser.openUrl(getContext(), songUrl);
            }
            return true;
        });

        if (cardData.getImageLocation() != null) {
            Bitmap coverBitmap = cardData.getCoverBitmap();
            this.imageView.setImage(cardData.getImageLocation(), null, coverBitmap != null ? new BitmapDrawable(getContext().getResources(), coverBitmap) : null, 0, null);
            if (cardData.getCoverBitmap() != null) {
                this.artistView.setTextColor(-1);
                this.nameView.setTextColor(-1);
                this.albumView.setTextColor(-1);
            } else {
                int themedColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText);
                this.artistView.setTextColor(themedColor);
                this.nameView.setTextColor(themedColor);
                this.albumView.setTextColor(themedColor);
            }
        } else {
            this.imageView.setImageResource(R.drawable.nocover, getThemedColor(Theme.key_player_button));
            int themedColor2 = getThemedColor(Theme.key_windowBackgroundWhiteBlackText);
            this.artistView.setTextColor(themedColor2);
            this.nameView.setTextColor(themedColor2);
            this.albumView.setTextColor(themedColor2);
        }

        String previewUrl2 = cardData.getNowPlayingDTO().getPreviewUrl();
        if (!TextUtils.equals(previewUrl2, this.currentPreviewUrl)) {
            this.currentPreviewUrl = previewUrl2;
            initializePlayer();
        }
        invalidate();
    }

    private void initializePlayer() {
        releasePlayer();
        if (nowPlayingCardData == null) {
            return;
        }
        String previewUrl = nowPlayingCardData.getNowPlayingDTO().getPreviewUrl();
        if (TextUtils.isEmpty(previewUrl)) {
            return;
        }
        ExoPlayer build = new ExoPlayer.Builder(getContext()).build();
        build.setMediaSource(new ProgressiveMediaSource.Factory(new DefaultDataSource.Factory(getContext()))
                .createMediaSource(MediaItem.fromUri(Uri.parse(previewUrl))));
        build.prepare();
        build.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                NowPlayingCard.this.isPlaying = isPlaying;
                NowPlayingCard.this.updatePlayPauseButton();
                if (!isPlaying) {
                    NowPlayingCard.this.abandonAudioFocus();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    NowPlayingCard.this.abandonAudioFocus();
                }
            }
        });
        this.player = build;
    }

    private void togglePlayPause() {
        if (player == null) {
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            abandonAudioFocus();
        } else if (requestAudioFocus()) {
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0L);
            }
            player.play();
        }
    }

    private boolean requestAudioFocus() {
        if (Build.VERSION.SDK_INT < 26) {
            return audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        AudioFocusRequest build = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build();
        this.audioFocusRequest = build;
        return audioManager.requestAudioFocus(build) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= 26) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
                audioFocusRequest = null;
            }
            return;
        }
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    private void updatePlayPauseButton() {
        if (playPauseDrawable != null) {
            playPauseDrawable.setPause(isPlaying);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        isPlaying = false;
        updatePlayPauseButton();
        abandonAudioFocus();
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (emoji != null) {
            emoji.attach();
        }
        if (player == null && nowPlayingCardData != null) {
            initializePlayer();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (emoji != null) {
            emoji.detach();
        }
        releasePlayer();
    }
}