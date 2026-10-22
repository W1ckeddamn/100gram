package com.exteragram.messenger.nowplaying.ui.components;

import android.graphics.Bitmap;

import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;

import com.exteragram.messenger.api.dto.NowPlayingDTO;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NowPlayingCardData {

    private final NowPlayingDTO nowPlayingDTO;
    private final Integer backgroundColor;
    private final Integer accentColor;
    private final Bitmap coverBitmap;
    private final ImageLocation imageLocation;
    private long userEmoji;

    public interface Callback {
        void onDataLoaded(NowPlayingCardData data);
    }

    public NowPlayingCardData(NowPlayingDTO nowPlayingDTO, Integer backgroundColor, Integer accentColor, Bitmap coverBitmap, ImageLocation imageLocation, long userEmoji) {
        this.nowPlayingDTO = nowPlayingDTO;
        this.backgroundColor = backgroundColor;
        this.accentColor = accentColor;
        this.coverBitmap = coverBitmap;
        this.imageLocation = imageLocation;
        this.userEmoji = userEmoji;
    }

    public NowPlayingDTO getNowPlayingDTO() {
        return nowPlayingDTO;
    }

    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    public Integer getAccentColor() {
        return accentColor;
    }

    public Bitmap getCoverBitmap() {
        return coverBitmap;
    }

    public ImageLocation getImageLocation() {
        return imageLocation;
    }

    public long getUserEmoji() {
        return userEmoji;
    }

    public void setUserEmoji(long userEmoji) {
        this.userEmoji = userEmoji;
    }

    public static void create(final NowPlayingDTO nowPlayingDTO, TLRPC.Document savedMusic, final Callback callback) {
        ImageLocation location = null;
        if ("TELEGRAM".equals(nowPlayingDTO.getPlatform()) && savedMusic != null && savedMusic.thumbs != null) {
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(savedMusic.thumbs, 1000);
            if (thumb != null) {
                location = ImageLocation.getForDocument(thumb, savedMusic);
            }
        }
        if (location == null && nowPlayingDTO.getCoverUrl() != null && !nowPlayingDTO.getCoverUrl().isEmpty()) {
            location = ImageLocation.getForPath(nowPlayingDTO.getCoverUrl());
        }

        final ImageLocation finalLocation = location;
        if (finalLocation == null) {
            AndroidUtilities.runOnUIThread(() -> callback.onDataLoaded(new NowPlayingCardData(nowPlayingDTO, null, null, null, null, 0L)));
            return;
        }

        final ImageReceiver imageReceiver = new ImageReceiver(null);
        final AtomicBoolean finished = new AtomicBoolean(false);

        final Runnable timeoutRunnable = () -> {
            if (finished.compareAndSet(false, true)) {
                imageReceiver.setDelegate(null);
                imageReceiver.onDetachedFromWindow();
                callback.onDataLoaded(new NowPlayingCardData(nowPlayingDTO, null, null, null, finalLocation, 0L));
            }
        };

        AndroidUtilities.runOnUIThread(() -> {
            imageReceiver.onAttachedToWindow();
            imageReceiver.setDelegate((imageReceiver1, set, thumb, memCache) -> {
                if (!set || thumb) {
                    return;
                }
                final Bitmap bitmap = imageReceiver1.getBitmap();
                Utilities.themeQueue.postRunnable(() -> {
                    int[] colors = extractColors(bitmap);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (finished.compareAndSet(false, true)) {
                            AndroidUtilities.cancelRunOnUIThread(timeoutRunnable);
                            imageReceiver.setDelegate(null);
                            imageReceiver.onDetachedFromWindow();
                            callback.onDataLoaded(new NowPlayingCardData(nowPlayingDTO, colors[0], colors[1], bitmap, finalLocation, 0L));
                        }
                    });
                });
            });
            AndroidUtilities.runOnUIThread(timeoutRunnable, 15000L);
            imageReceiver.setImage(finalLocation, null, null, null, null, 0);
        });
    }

    private static int adjustHsl(int color, float luminance, float saturation) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        if (saturation > 0.0f) {
            hsl[1] = Math.min(hsl[1] * saturation, 1.0f);
        }
        hsl[2] = Math.min(hsl[2] * luminance, 1.0f);
        return ColorUtils.HSLToColor(hsl);
    }

    private static int[] extractColors(Bitmap bitmap) {
        if (bitmap == null) {
            return new int[]{0, 0};
        }
        Palette palette = Palette.from(bitmap).generate();
        int color;
        Palette.Swatch darkVibrant = palette.getDarkVibrantSwatch();
        if (darkVibrant != null) {
            color = darkVibrant.getRgb();
        } else {
            Palette.Swatch muted = palette.getMutedSwatch();
            if (muted != null) {
                color = muted.getRgb();
            } else {
                Palette.Swatch darkMuted = palette.getDarkMutedSwatch();
                if (darkMuted != null) {
                    color = darkMuted.getRgb();
                } else {
                    Palette.Swatch dominant = palette.getDominantSwatch();
                    color = dominant != null ? dominant.getRgb() : AndroidUtilities.getDominantColor(bitmap);
                }
            }
        }

        double contrast = ColorUtils.calculateContrast(-1, color);
        if (contrast > 15.0d) {
            color = adjustHsl(color, 2.0f, -1.0f);
        } else if (contrast < 10.0d) {
            color = adjustHsl(color, 0.5f, -1.0f);
        }
        if (ColorUtils.calculateContrast(-1, color) < 3.0d) {
            color = ColorUtils.blendARGB(color, 0xFF000000, 0.3f);
        }

        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        float lum = hsl[2];
        float f;
        if (lum <= 0.25f) {
            f = 2.0f;
        } else if (lum <= 0.5f) {
            f = 1.5f;
        } else if (lum <= 0.75f) {
            f = 1.0f;
        } else {
            f = 0.5f;
        }

        int accent = adjustHsl(color, f, -1.0f);
        return new int[]{color, accent};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NowPlayingCardData that = (NowPlayingCardData) o;
        return userEmoji == that.userEmoji &&
                Objects.equals(nowPlayingDTO, that.nowPlayingDTO) &&
                Objects.equals(backgroundColor, that.backgroundColor) &&
                Objects.equals(accentColor, that.accentColor) &&
                Objects.equals(coverBitmap, that.coverBitmap) &&
                Objects.equals(imageLocation, that.imageLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nowPlayingDTO, backgroundColor, accentColor, coverBitmap, imageLocation, userEmoji);
    }
}