package org.telegram.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint("ViewConstructor")
public class AvatarCornersPreviewCell extends FrameLayout {

    private final AltSeekbar seekBar;
    private final FrameLayout preview;
    private final Paint mockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint brightMockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AvatarCornersPreviewCell(Context context, BaseFragment fragment) {
        super(context);
        setWillNotDraw(false);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        mockPaint.setColor(PreviewColors.getMockColor(false));
        brightMockPaint.setColor(PreviewColors.getMockColor(true));

        preview = new FrameLayout(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                drawMock(canvas);
            }
        };
        preview.setWillNotDraw(false);
        preview.setBackground(new PreviewBackgroundDrawable(12.0f));
        preview.setMinimumHeight(AndroidUtilities.dp(84.0f));

        seekBar = new AltSeekbar(
                context,
                value -> {
                    HundredGramConfig.setAvatarCorners(value);
                    if (preview != null) {
                        preview.invalidate();
                    }
                    if (fragment != null && fragment.getParentLayout() != null) {
                        fragment.getParentLayout().rebuildFragments(0);
                    }
                },
                0,
                28,
                LocaleController.getString("AvatarCorners", R.string.AvatarCorners),
                LocaleController.getString("AvatarCornersLeft", R.string.AvatarCornersLeft),
                LocaleController.getString("AvatarCornersRight", R.string.AvatarCornersRight)
        );
        seekBar.setProgress(HundredGramConfig.avatarCorners);
        addView(seekBar, LayoutHelper.createFrame(-1, -2.0f));
        addView(preview, LayoutHelper.createFrame(-1, -2.0f, 49, 16.0f, 108.0f, 16.0f, 16.0f));
    }

    public void updateSliderStyle() {
        if (seekBar != null) {
            seekBar.updateSliderStyle();
        }
    }

    private void drawMock(Canvas canvas) {
        mockPaint.setColor(PreviewColors.getMockColor(false));
        brightMockPaint.setColor(PreviewColors.getMockColor(true));

        float dp2 = AndroidUtilities.dp(15.0f);
        float dp3 = AndroidUtilities.dp(82.0f);
        int avatarSize = AndroidUtilities.dp(56.0f);
        float width = preview.getWidth();
        RectF rectF = AndroidUtilities.rectTmp;

        float top = AndroidUtilities.dp(14.0f);
        float right = dp2 + avatarSize;
        float bottom = top + avatarSize;
        rectF.set(dp2, top, right, bottom);

        float avatarRadius = HundredGramConfig.getAvatarCorners(56.0f);

        float squareness = Math.max(0.0f, Math.min(1.0f, 1.0f - (HundredGramConfig.avatarCorners / 28.0f)));
        float dotOuterRadius = AndroidUtilities.dpf2(2.0f * squareness + 7.0f);
        float dotInnerRadius = AndroidUtilities.dpf2(squareness + 5.0f);

        float dotOffset = AndroidUtilities.dpf2(8.0f);
        float dotCenterX = right - dotOffset;
        float dotCenterY = bottom - dotOffset;

        canvas.save();
        Path clipPath = new Path();
        clipPath.addCircle(dotCenterX, dotCenterY, dotOuterRadius, Path.Direction.CCW);
        canvas.clipPath(clipPath, Region.Op.DIFFERENCE);

        if (avatarRadius <= 0) {
            canvas.drawRect(rectF, brightMockPaint);
        } else {
            canvas.drawRoundRect(rectF, avatarRadius, avatarRadius, brightMockPaint);
        }
        canvas.restore();

        Theme.dialogs_onlineCirclePaint.setColor(Theme.getColor(Theme.key_chats_onlineCircle));
        canvas.drawCircle(dotCenterX, dotCenterY, dotInnerRadius, Theme.dialogs_onlineCirclePaint);

        float lineRadius = AndroidUtilities.dp(4.0f);
        rectF.set(dp3, AndroidUtilities.dp(16.0f), dp3 + width * 0.38f, AndroidUtilities.dp(24.0f));
        canvas.drawRoundRect(rectF, lineRadius, lineRadius, brightMockPaint);

        rectF.set(dp3, AndroidUtilities.dp(36.0f), dp3 + width * 0.52f, AndroidUtilities.dp(44.0f));
        canvas.drawRoundRect(rectF, lineRadius, lineRadius, mockPaint);

        rectF.set(dp3, AndroidUtilities.dp(54.0f), dp3 + width * 0.34f, AndroidUtilities.dp(62.0f));
        canvas.drawRoundRect(rectF, lineRadius, lineRadius, mockPaint);

        rectF.set(width - AndroidUtilities.dp(56.0f), AndroidUtilities.dp(16.0f), width - AndroidUtilities.dp(16.0f), AndroidUtilities.dp(24.0f));
        canvas.drawRoundRect(rectF, lineRadius, lineRadius, mockPaint);
    }

    public void updateProgress() {
        seekBar.setProgress(HundredGramConfig.avatarCorners);
        preview.invalidate();
    }
}
