package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.camera.camerax.CameraXUtils;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramCameraActivity extends BaseFragment {

    private static final int ITEM_CAMERA_ENGINE = 1;
    private static final int ITEM_PERFORMANCE_MODE = 2;
    private static final int ITEM_QUALITY = 3;
    private static final int ITEM_FPS = 4;
    private static final int ITEM_DEFAULT_LENS = 5;
    private static final int ITEM_WIDE_ANGLE = 6;
    private static final int ITEM_ZOOM_SLIDER = 7;
    private static final int ITEM_STABILIZATION = 8;
    private static final int ITEM_HDR = 9;

    // Round Video
    private static final int ITEM_ROUND_START_CAMERA = 10;
    private static final int ITEM_ROUND_RESOLUTION = 11;
    private static final int ITEM_ROUND_FPS = 12;
    private static final int ITEM_ROUND_HIGH_BITRATE = 13;
    private static final int ITEM_ROUND_CODEC = 14;

    private UniversalRecyclerView listView;
    private CameraXUtils.CameraReport cameraReport;

    @Override
    public boolean onFragmentCreate() {
        cameraReport = CameraXUtils.diagnoseCamera(getParentActivity());
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("CameraEngineTitle", R.string.CameraEngineTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, null);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private View createBadgeView(Context context, String text, boolean supported) {
        TextView badge = new TextView(context);
        badge.setText((supported ? "✓ " : "✕ ") + text);
        badge.setTextSize(12);
        badge.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        badge.setTextColor(supported ? Color.parseColor("#4CAF50") : Color.parseColor("#9E9E9E"));
        badge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6),
                supported ? Color.parseColor("#1A4CAF50") : Color.parseColor("#15000000")));
        badge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
        return badge;
    }

    private View createCapabilitiesCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView title = new TextView(context);
        title.setText(LocaleController.getString("CameraDiagnostics", R.string.CameraDiagnostics));
        title.setTextSize(15);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Badges grid
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(createBadgeView(context, "CameraX", cameraReport == null || cameraReport.cameraXSupported));
        View space1 = new View(context);
        row1.addView(space1, LayoutHelper.createLinear(8, 0));
        row1.addView(createBadgeView(context, "4K UHD (2160p)", cameraReport != null && cameraReport.can4K));
        View space2 = new View(context);
        row1.addView(space2, LayoutHelper.createLinear(8, 0));
        row1.addView(createBadgeView(context, "60 FPS", cameraReport != null && cameraReport.can60Fps));
        card.addView(row1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(createBadgeView(context, "Широкоугольная (0.6x)", cameraReport != null && cameraReport.wideAngleAvailable));
        View space3 = new View(context);
        row2.addView(space3, LayoutHelper.createLinear(8, 0));
        row2.addView(createBadgeView(context, "OIS / EIS", cameraReport != null && cameraReport.stabilizationAvailable));
        View space4 = new View(context);
        row2.addView(space4, LayoutHelper.createLinear(8, 0));
        row2.addView(createBadgeView(context, "HDR", cameraReport != null && cameraReport.hdrAvailable));
        card.addView(row2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // Auto-tune button
        FrameLayout btnLayout = new FrameLayout(context);
        btnLayout.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(10), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        btnLayout.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        TextView btnText = new TextView(context);
        btnText.setText("🎯 " + LocaleController.getString("CameraAutoTune", R.string.CameraAutoTune));
        btnText.setTextSize(13);
        btnText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        btnText.setTextColor(Color.WHITE);
        btnText.setGravity(Gravity.CENTER);
        btnLayout.addView(btnText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        btnLayout.setOnClickListener(v -> autoTuneSettings());
        card.addView(btnLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        FrameLayout container = new FrameLayout(context);
        container.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        container.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return container;
    }

    private void autoTuneSettings() {
        HundredGramConfig.setCameraEngine(HundredGramConfig.CAMERA_ENGINE_CAMERAX);
        HundredGramConfig.cameraPerformanceMode = false;
        if (cameraReport != null && cameraReport.can4K) {
            HundredGramConfig.setCameraQuality(2160);
        } else {
            HundredGramConfig.setCameraQuality(1080);
        }
        if (cameraReport != null && cameraReport.can60Fps) {
            HundredGramConfig.setCameraFps(60);
            HundredGramConfig.setCameraRoundFps(60);
        }
        HundredGramConfig.toggleCameraRoundHighBitrate();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        BulletinFactory.of(this)
                .createSimpleBulletin(R.drawable.msg_camera, LocaleController.getString("CameraAutoTuneDone", R.string.CameraAutoTuneDone))
                .show();
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Capabilities diagnostic card
        items.add(UItem.asCustom(createCapabilitiesCard(getContext())));

        // Camera Engine Selector
        items.add(UItem.asHeader(LocaleController.getString("CameraEngineChoose", R.string.CameraEngineChoose)));
        String engineName = HundredGramConfig.cameraEngine == HundredGramConfig.CAMERA_ENGINE_CAMERAX
                ? LocaleController.getString("CameraEngineCameraX", R.string.CameraEngineCameraX)
                : LocaleController.getString("CameraEngineSystem", R.string.CameraEngineSystem);
        items.add(UItem.asButton(ITEM_CAMERA_ENGINE, LocaleController.getString("CameraEngineChoose", R.string.CameraEngineChoose), engineName));
        items.add(UItem.asShadow("Jetpack CameraX обеспечивает расширенные возможности зума, 60 FPS и широкоугольной съемки"));

        // CameraX Settings Section
        if (HundredGramConfig.cameraEngine == HundredGramConfig.CAMERA_ENGINE_CAMERAX) {
            items.add(UItem.asHeader("Настройки CameraX"));

            items.add(UItem.asCheck(ITEM_PERFORMANCE_MODE, LocaleController.getString("CameraPerformanceMode", R.string.CameraPerformanceMode))
                    .setChecked(HundredGramConfig.cameraPerformanceMode));
            items.add(UItem.asShadow(LocaleController.getString("CameraPerformanceModeInfo", R.string.CameraPerformanceModeInfo)));

            String qualityName;
            switch (HundredGramConfig.cameraQuality) {
                case 2160: qualityName = "2160p (4K Ultra HD)"; break;
                case 1440: qualityName = "1440p (2K Quad HD)"; break;
                case 720: qualityName = "720p (HD)"; break;
                default: qualityName = "1080p (Full HD)"; break;
            }
            items.add(UItem.asButton(ITEM_QUALITY, LocaleController.getString("CameraQuality", R.string.CameraQuality), qualityName));

            String fpsName;
            switch (HundredGramConfig.cameraFps) {
                case 60: fpsName = "60 FPS"; break;
                case 30: fpsName = "30 FPS"; break;
                case 24: fpsName = "24 FPS"; break;
                default: fpsName = LocaleController.getString("CameraFpsAuto", R.string.CameraFpsAuto); break;
            }
            items.add(UItem.asButton(ITEM_FPS, LocaleController.getString("CameraFps", R.string.CameraFps), fpsName));

            String lensName;
            switch (HundredGramConfig.cameraDefaultLens) {
                case 1: lensName = LocaleController.getString("CameraDefaultLensFront", R.string.CameraDefaultLensFront); break;
                case 2: lensName = LocaleController.getString("CameraDefaultLensLast", R.string.CameraDefaultLensLast); break;
                default: lensName = LocaleController.getString("CameraDefaultLensBack", R.string.CameraDefaultLensBack); break;
            }
            items.add(UItem.asButton(ITEM_DEFAULT_LENS, LocaleController.getString("CameraDefaultLens", R.string.CameraDefaultLens), lensName));

            items.add(UItem.asCheck(ITEM_WIDE_ANGLE, "Широкоугольный объектив по умолчанию")
                    .setChecked(HundredGramConfig.cameraXWideAngle));
            items.add(UItem.asShadow("Автоматически включает ультраширокоугольный модуль при открытии видоискателя"));

            items.add(UItem.asCheck(ITEM_ZOOM_SLIDER, "Ползунок зума")
                    .setChecked(HundredGramConfig.cameraXZoomSlider));
            items.add(UItem.asShadow("Отображает удобный слайдер масштабирования"));

            items.add(UItem.asCheck(ITEM_STABILIZATION, LocaleController.getString("CameraStabilization", R.string.CameraStabilization))
                    .setChecked(HundredGramConfig.cameraStabilization));
            items.add(UItem.asShadow(LocaleController.getString("CameraStabilizationInfo", R.string.CameraStabilizationInfo)));

            String hdrName;
            switch (HundredGramConfig.cameraHdr) {
                case 2: hdrName = LocaleController.getString("CameraHdrOn", R.string.CameraHdrOn); break;
                case 0: hdrName = LocaleController.getString("CameraHdrOff", R.string.CameraHdrOff); break;
                default: hdrName = LocaleController.getString("CameraHdrAuto", R.string.CameraHdrAuto); break;
            }
            items.add(UItem.asButton(ITEM_HDR, LocaleController.getString("CameraHdr", R.string.CameraHdr), hdrName));
        }

        // Round Video Messages (Кружочки) Section
        items.add(UItem.asHeader(LocaleController.getString("RoundVideoHeader", R.string.RoundVideoHeader)));

        String startCamName;
        switch (HundredGramConfig.cameraRoundStartCamera) {
            case HundredGramConfig.CAMERA_ROUND_START_BACK:
                startCamName = LocaleController.getString("RoundCameraBack", R.string.RoundCameraBack);
                break;
            case HundredGramConfig.CAMERA_ROUND_START_MENU:
                startCamName = LocaleController.getString("RoundCameraMenu", R.string.RoundCameraMenu);
                break;
            case HundredGramConfig.CAMERA_ROUND_START_LAST_USED:
                startCamName = LocaleController.getString("RoundCameraLast", R.string.RoundCameraLast);
                break;
            default:
                startCamName = LocaleController.getString("RoundCameraFront", R.string.RoundCameraFront);
                break;
        }
        items.add(UItem.asButton(ITEM_ROUND_START_CAMERA, LocaleController.getString("RoundCameraStart", R.string.RoundCameraStart), startCamName));
        items.add(UItem.asShadow("Выбор объектива для начала записи: фронталка, задняя, быстрое меню выбора перед записью или последняя использованная"));

        String resName;
        switch (HundredGramConfig.cameraRoundResolution) {
            case 720: resName = "720p HD"; break;
            case 480: resName = "480p"; break;
            default: resName = "360p (Telegram)"; break;
        }
        items.add(UItem.asButton(ITEM_ROUND_RESOLUTION, LocaleController.getString("RoundVideoResolution", R.string.RoundVideoResolution), resName));

        items.add(UItem.asCheck(ITEM_ROUND_FPS, LocaleController.getString("RoundVideoFps", R.string.RoundVideoFps))
                .setChecked(HundredGramConfig.cameraRoundFps == 60 || HundredGramConfig.cameraX60Fps));
        items.add(UItem.asShadow(LocaleController.getString("RoundVideoFpsInfo", R.string.RoundVideoFpsInfo)));

        items.add(UItem.asCheck(ITEM_ROUND_HIGH_BITRATE, LocaleController.getString("RoundVideoHighBitrate", R.string.RoundVideoHighBitrate))
                .setChecked(HundredGramConfig.cameraRoundHighBitrate));
        items.add(UItem.asShadow(LocaleController.getString("RoundVideoHighBitrateInfo", R.string.RoundVideoHighBitrateInfo)));

        String codecName = HundredGramConfig.cameraRoundCodec == 1 ? "H.265 (HEVC)" : "H.264 (AVC)";
        items.add(UItem.asButton(ITEM_ROUND_CODEC, LocaleController.getString("RoundVideoCodec", R.string.RoundVideoCodec), codecName));
        items.add(UItem.asShadow("H.265 обеспечивает более высокую четкость при том же размере файла"));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_CAMERA_ENGINE:
                showCameraEngineDialog();
                break;
            case ITEM_PERFORMANCE_MODE:
                HundredGramConfig.toggleCameraPerformanceMode();
                item.setChecked(HundredGramConfig.cameraPerformanceMode);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_QUALITY:
                showQualityDialog();
                break;
            case ITEM_FPS:
                showFpsDialog();
                break;
            case ITEM_DEFAULT_LENS:
                showDefaultLensDialog();
                break;
            case ITEM_WIDE_ANGLE:
                HundredGramConfig.toggleCameraXWideAngle();
                item.setChecked(HundredGramConfig.cameraXWideAngle);
                break;
            case ITEM_ZOOM_SLIDER:
                HundredGramConfig.toggleCameraXZoomSlider();
                item.setChecked(HundredGramConfig.cameraXZoomSlider);
                break;
            case ITEM_STABILIZATION:
                HundredGramConfig.toggleCameraStabilization();
                item.setChecked(HundredGramConfig.cameraStabilization);
                break;
            case ITEM_HDR:
                showHdrDialog();
                break;
            case ITEM_ROUND_START_CAMERA:
                showRoundStartCameraDialog();
                break;
            case ITEM_ROUND_RESOLUTION:
                showRoundResolutionDialog();
                break;
            case ITEM_ROUND_FPS:
                int targetFps = (HundredGramConfig.cameraRoundFps == 60 || HundredGramConfig.cameraX60Fps) ? 30 : 60;
                HundredGramConfig.setCameraRoundFps(targetFps);
                item.setChecked(targetFps == 60);
                break;
            case ITEM_ROUND_HIGH_BITRATE:
                HundredGramConfig.toggleCameraRoundHighBitrate();
                item.setChecked(HundredGramConfig.cameraRoundHighBitrate);
                break;
            case ITEM_ROUND_CODEC:
                showRoundCodecDialog();
                break;
        }
    }

    private void showCameraEngineDialog() {
        CharSequence[] options = new CharSequence[]{
                LocaleController.getString("CameraEngineSystem", R.string.CameraEngineSystem),
                LocaleController.getString("CameraEngineCameraX", R.string.CameraEngineCameraX)
        };
        new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("CameraEngineChoose", R.string.CameraEngineChoose))
                .setItems(options, (dialog, which) -> {
                    HundredGramConfig.setCameraEngine(which);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .show();
    }

    private void showQualityDialog() {
        CharSequence[] options = new CharSequence[]{"2160p (4K UHD)", "1440p (2K)", "1080p (Full HD)", "720p (HD)"};
        new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("CameraQuality", R.string.CameraQuality))
                .setItems(options, (dialog, which) -> {
                    int q;
                    if (which == 0) {
                        q = 2160;
                    } else if (which == 1) {
                        q = 1440;
                    } else if (which == 3) {
                        q = 720;
                    } else {
                        q = 1080;
                    }
                    HundredGramConfig.setCameraQuality(q);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .show();
    }

    private void showFpsDialog() {
        CharSequence[] options = new CharSequence[]{
                LocaleController.getString("CameraFpsAuto", R.string.CameraFpsAuto),
                "60 FPS",
                "30 FPS",
                "24 FPS"
        };
        new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("CameraFps", R.string.CameraFps))
                .setItems(options, (dialog, which) -> {
                    int fps;
                    if (which == 1) {
                        fps = 60;
                    } else if (which == 2) {
                        fps = 30;
                    } else if (which == 3) {
                        fps = 24;
                    } else {
                        fps = 0;
                    }
                    HundredGramConfig.setCameraFps(fps);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .show();
    }

    private void showDefaultLensDialog() {
        CharSequence[] options = new CharSequence[]{
                LocaleController.getString("CameraDefaultLensBack", R.string.CameraDefaultLensBack),
                LocaleController.getString("CameraDefaultLensFront", R.string.CameraDefaultLensFront),
                LocaleController.getString("CameraDefaultLensLast", R.string.CameraDefaultLensLast)
        };
        new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("CameraDefaultLens", R.string.CameraDefaultLens))
                .setItems(options, (dialog, which) -> {
                    HundredGramConfig.setCameraDefaultLens(which);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .show();
    }

    private void showHdrDialog() {
        CharSequence[] options = new CharSequence[]{
                LocaleController.getString("CameraHdrAuto", R.string.CameraHdrAuto),
                LocaleController.getString("CameraHdrOn", R.string.CameraHdrOn),
                LocaleController.getString("CameraHdrOff", R.string.CameraHdrOff)
        };
        new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("CameraHdr", R.string.CameraHdr))
                .setItems(options, (dialog, which) -> {
                    int hdr = which == 1 ? 2 : (which == 2 ? 0 : 1);
                    HundredGramConfig.setCameraHdr(hdr);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .show();
    }

    private void showRoundStartCameraDialog() {
        CharSequence[] options = new CharSequence[]{
                LocaleController.getString("RoundCameraFront", R.string.RoundCameraFront),
                LocaleController.getString("RoundCameraBack", R.string.RoundCameraBack),
                LocaleController.getString("RoundCameraMenu", R.string.RoundCameraMenu),
                LocaleController.getString("RoundCameraLast", R.string.RoundCameraLast)
        };
        new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("RoundCameraStart", R.string.RoundCameraStart))
                .setItems(options, (dialog, which) -> {
                    HundredGramConfig.setCameraRoundStartCamera(which);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .show();
    }

    private void showRoundResolutionDialog() {
        CharSequence[] options = new CharSequence[]{"360p (Стандарт)", "480p (Четкое)", "720p HD (Высокое качество)"};
        new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("RoundVideoResolution", R.string.RoundVideoResolution))
                .setItems(options, (dialog, which) -> {
                    int res = which == 2 ? 720 : (which == 1 ? 480 : 360);
                    HundredGramConfig.setCameraRoundResolution(res);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .show();
    }

    private void showRoundCodecDialog() {
        CharSequence[] options = new CharSequence[]{
                LocaleController.getString("RoundVideoCodecH264", R.string.RoundVideoCodecH264),
                LocaleController.getString("RoundVideoCodecH265", R.string.RoundVideoCodecH265)
        };
        new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("RoundVideoCodec", R.string.RoundVideoCodec))
                .setItems(options, (dialog, which) -> {
                    HundredGramConfig.setCameraRoundCodec(which);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
