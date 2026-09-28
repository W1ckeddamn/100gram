package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.StatsController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SlideChooseView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class HundredGramNetworkBoostActivity extends BaseFragment {

    private static final int ITEM_SLIDER_DOWNLOAD = 1;
    private static final int ITEM_AUTO_MODE = 2;
    private static final int ITEM_UPLOAD_BOOST = 3;
    private static final int ITEM_UPLOAD_FILE_SIZE_AWARE = 4;

    private UniversalRecyclerView listView;
    private SlideChooseView downloadSlider;
    private TextView downloadSpeedText;
    private TextView uploadSpeedText;
    private TextView activeThreadsText;
    private TextView speedTestResultText;
    private View speedTestButton;
    private boolean isTestingSpeed;

    private final Runnable statsUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (getContext() == null || listView == null) {
                return;
            }
            updateLiveStats();
            AndroidUtilities.runOnUIThread(this, 1000);
        }
    };

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("NetworkBoostTitle", R.string.NetworkBoostTitle));
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

    private View createMonitorCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));

        TextView title = new TextView(context);
        title.setText(LocaleController.getString("NetworkBoostLiveStats", R.string.NetworkBoostLiveStats));
        title.setTextSize(15);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        LinearLayout statsRow = new LinearLayout(context);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(3f);

        // Download column
        LinearLayout dlCol = new LinearLayout(context);
        dlCol.setOrientation(LinearLayout.VERTICAL);
        TextView dlLabel = new TextView(context);
        dlLabel.setText("⬇ " + LocaleController.getString("NetworkBoostDownloadRate", R.string.NetworkBoostDownloadRate));
        dlLabel.setTextSize(12);
        dlLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        downloadSpeedText = new TextView(context);
        downloadSpeedText.setText("0.0 MB/s");
        downloadSpeedText.setTextSize(16);
        downloadSpeedText.setTypeface(Typeface.DEFAULT_BOLD);
        downloadSpeedText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
        dlCol.addView(dlLabel);
        dlCol.addView(downloadSpeedText);
        statsRow.addView(dlCol, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1f));

        // Upload column
        LinearLayout ulCol = new LinearLayout(context);
        ulCol.setOrientation(LinearLayout.VERTICAL);
        TextView ulLabel = new TextView(context);
        ulLabel.setText("⬆ " + LocaleController.getString("NetworkBoostUploadRate", R.string.NetworkBoostUploadRate));
        ulLabel.setTextSize(12);
        ulLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        uploadSpeedText = new TextView(context);
        uploadSpeedText.setText("0.0 MB/s");
        uploadSpeedText.setTextSize(16);
        uploadSpeedText.setTypeface(Typeface.DEFAULT_BOLD);
        uploadSpeedText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
        ulCol.addView(ulLabel);
        ulCol.addView(uploadSpeedText);
        statsRow.addView(ulCol, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1f));

        // Threads column
        LinearLayout thCol = new LinearLayout(context);
        thCol.setOrientation(LinearLayout.VERTICAL);
        TextView thLabel = new TextView(context);
        thLabel.setText("⚡ " + LocaleController.getString("NetworkBoostActiveThreads", R.string.NetworkBoostActiveThreads));
        thLabel.setTextSize(12);
        thLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        activeThreadsText = new TextView(context);
        activeThreadsText.setText("0");
        activeThreadsText.setTextSize(16);
        activeThreadsText.setTypeface(Typeface.DEFAULT_BOLD);
        activeThreadsText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
        thCol.addView(thLabel);
        thCol.addView(activeThreadsText);
        statsRow.addView(thCol, new LinearLayout.LayoutParams(0, LayoutHelper.WRAP_CONTENT, 1f));

        card.addView(statsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Speed test button & result
        FrameLayout btnLayout = new FrameLayout(context);
        btnLayout.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(10), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        btnLayout.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        TextView btnText = new TextView(context);
        btnText.setText("⚡ " + LocaleController.getString("NetworkBoostSpeedTest", R.string.NetworkBoostSpeedTest));
        btnText.setTextSize(14);
        btnText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        btnText.setTextColor(Color.WHITE);
        btnText.setGravity(Gravity.CENTER);
        btnLayout.addView(btnText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        speedTestButton = btnLayout;
        speedTestButton.setOnClickListener(v -> runSpeedTest());
        card.addView(btnLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        speedTestResultText = new TextView(context);
        speedTestResultText.setTextSize(12);
        speedTestResultText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        speedTestResultText.setGravity(Gravity.CENTER);
        speedTestResultText.setVisibility(View.GONE);
        card.addView(speedTestResultText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        FrameLayout container = new FrameLayout(context);
        container.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        container.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return container;
    }

    private View createDownloadSliderView(Context context) {
        FrameLayout container = new FrameLayout(context);
        container.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        downloadSlider = new SlideChooseView(context);
        int currentOption;
        switch (HundredGramConfig.downloadBoost) {
            case HundredGramConfig.DOWNLOAD_BOOST_ULTRA:
                currentOption = 2;
                break;
            case HundredGramConfig.DOWNLOAD_BOOST_FAST:
                currentOption = 1;
                break;
            default:
                currentOption = 0;
                break;
        }
        downloadSlider.setOptions(currentOption,
                LocaleController.getString("NetworkBoostDownloadOff", R.string.NetworkBoostDownloadOff),
                LocaleController.getString("NetworkBoostDownloadFast", R.string.NetworkBoostDownloadFast),
                LocaleController.getString("NetworkBoostDownloadUltra", R.string.NetworkBoostDownloadUltra)
        );
        downloadSlider.setCallback(index -> {
            int newMode;
            if (index == 2) {
                newMode = HundredGramConfig.DOWNLOAD_BOOST_ULTRA;
            } else if (index == 1) {
                newMode = HundredGramConfig.DOWNLOAD_BOOST_FAST;
            } else {
                newMode = HundredGramConfig.DOWNLOAD_BOOST_OFF;
            }
            HundredGramConfig.setDownloadBoost(newMode);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        container.addView(downloadSlider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 6, 4, 6, 4));
        return container;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Live Monitor Card
        items.add(UItem.asCustom(createMonitorCard(getContext())));

        // Download Boost Section
        items.add(UItem.asHeader(LocaleController.getString("NetworkBoostDownloadSpeed", R.string.NetworkBoostDownloadSpeed)));
        items.add(UItem.asCustom(ITEM_SLIDER_DOWNLOAD, createDownloadSliderView(getContext())));
        String sliderSub;
        if (HundredGramConfig.downloadBoost == HundredGramConfig.DOWNLOAD_BOOST_ULTRA) {
            sliderSub = "Режим «Ультра»: 12 параллельных потоков MTProto, чанки по 1 МБ (максимальная утилизация гигабитного канала)";
        } else if (HundredGramConfig.downloadBoost == HundredGramConfig.DOWNLOAD_BOOST_FAST) {
            sliderSub = "Режим «Быстро»: 8 параллельных потоков, чанки по 512 КБ (оптимально для 4G/LTE сетей)";
        } else {
            sliderSub = "Стандартный режим Telegram: 4 параллельных потока, чанки по 128 КБ";
        }
        items.add(UItem.asShadow(sliderSub));

        items.add(UItem.asCheck(ITEM_AUTO_MODE, LocaleController.getString("NetworkBoostAuto", R.string.NetworkBoostAuto))
                .setChecked(HundredGramConfig.downloadBoost == HundredGramConfig.DOWNLOAD_BOOST_AUTO));
        items.add(UItem.asShadow(LocaleController.getString("NetworkBoostAutoInfo", R.string.NetworkBoostAutoInfo)));

        // Upload Boost Section
        items.add(UItem.asHeader(LocaleController.getString("NetworkBoostUpload", R.string.NetworkBoostUpload)));
        items.add(UItem.asCheck(ITEM_UPLOAD_BOOST, LocaleController.getString("NetworkBoostUpload", R.string.NetworkBoostUpload))
                .setChecked(HundredGramConfig.uploadBoost != HundredGramConfig.UPLOAD_BOOST_OFF));
        items.add(UItem.asShadow(LocaleController.getString("NetworkBoostUploadInfo", R.string.NetworkBoostUploadInfo)));

        items.add(UItem.asCheck(ITEM_UPLOAD_FILE_SIZE_AWARE, LocaleController.getString("NetworkBoostUploadAdaptive", R.string.NetworkBoostUploadAdaptive))
                .setChecked(HundredGramConfig.uploadBoostFileSizeAware));
        items.add(UItem.asShadow(LocaleController.getString("NetworkBoostUploadAdaptiveInfo", R.string.NetworkBoostUploadAdaptiveInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_AUTO_MODE:
                if (HundredGramConfig.downloadBoost == HundredGramConfig.DOWNLOAD_BOOST_AUTO) {
                    HundredGramConfig.setDownloadBoost(HundredGramConfig.DOWNLOAD_BOOST_FAST);
                } else {
                    HundredGramConfig.setDownloadBoost(HundredGramConfig.DOWNLOAD_BOOST_AUTO);
                }
                item.setChecked(HundredGramConfig.downloadBoost == HundredGramConfig.DOWNLOAD_BOOST_AUTO);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_UPLOAD_BOOST:
                if (HundredGramConfig.uploadBoost == HundredGramConfig.UPLOAD_BOOST_OFF) {
                    HundredGramConfig.setUploadBoost(HundredGramConfig.UPLOAD_BOOST_AUTO);
                } else {
                    HundredGramConfig.setUploadBoost(HundredGramConfig.UPLOAD_BOOST_OFF);
                }
                item.setChecked(HundredGramConfig.uploadBoost != HundredGramConfig.UPLOAD_BOOST_OFF);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_UPLOAD_FILE_SIZE_AWARE:
                HundredGramConfig.toggleUploadBoostFileSizeAware();
                item.setChecked(HundredGramConfig.uploadBoostFileSizeAware);
                break;
        }
    }

    private void updateLiveStats() {
        if (downloadSpeedText != null) {
            float dl = HundredGramConfig.getDownloadSpeedMBps();
            downloadSpeedText.setText(String.format(Locale.US, "%.1f MB/s", dl));
        }
        if (uploadSpeedText != null) {
            float ul = HundredGramConfig.getUploadSpeedMBps();
            uploadSpeedText.setText(String.format(Locale.US, "%.1f MB/s", ul));
        }
        if (activeThreadsText != null) {
            int threads = HundredGramConfig.getActiveDownloadThreads() + HundredGramConfig.getActiveUploadThreads();
            activeThreadsText.setText(String.valueOf(threads));
        }
    }

    private void runSpeedTest() {
        if (isTestingSpeed) return;
        isTestingSpeed = true;
        if (speedTestResultText != null) {
            speedTestResultText.setText(LocaleController.getString("NetworkBoostSpeedTesting", R.string.NetworkBoostSpeedTesting));
            speedTestResultText.setVisibility(View.VISIBLE);
        }

        final long start = SystemClock.elapsedRealtime();
        ConnectionsManager.getInstance(currentAccount).sendRequest(new org.telegram.tgnet.TLRPC.TL_help_getNearestDc(), (response, error) -> {
            long ping = SystemClock.elapsedRealtime() - start;
            AndroidUtilities.runOnUIThread(() -> {
                isTestingSpeed = false;
                int netType = ApplicationLoader.getAutodownloadNetworkType();
                String recommended;
                if (netType == StatsController.TYPE_WIFI || ping < 60) {
                    recommended = "Ультра (12 потоков, 1 МБ)";
                    HundredGramConfig.setDownloadBoost(HundredGramConfig.DOWNLOAD_BOOST_ULTRA);
                    HundredGramConfig.setUploadBoost(HundredGramConfig.UPLOAD_BOOST_ULTRA);
                } else {
                    recommended = "Быстро (8 потоков, 512 КБ)";
                    HundredGramConfig.setDownloadBoost(HundredGramConfig.DOWNLOAD_BOOST_FAST);
                    HundredGramConfig.setUploadBoost(HundredGramConfig.UPLOAD_BOOST_FAST);
                }
                if (speedTestResultText != null) {
                    speedTestResultText.setText(String.format(Locale.getDefault(),
                            LocaleController.getString("NetworkBoostSpeedTestResult", R.string.NetworkBoostSpeedTestResult),
                            (int) Math.max(12, ping), recommended));
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                BulletinFactory.of(HundredGramNetworkBoostActivity.this)
                        .createSimpleBulletin(R.drawable.msg_download, "Оптимальный режим сети применён: " + recommended)
                        .show();
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLiveStats();
        AndroidUtilities.runOnUIThread(statsUpdateRunnable, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        AndroidUtilities.cancelRunOnUIThread(statsUpdateRunnable);
    }
}
