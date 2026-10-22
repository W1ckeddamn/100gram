package org.telegram.ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramActivitySimulator;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramActivitySimulatorActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ITEM_STATUS_CARD = 1;
    private static final int ITEM_QUICK_DURATION = 2;
    private static final int ITEM_QUICK_START = 3;
    private static final int ITEM_SCHEDULE_ENABLED = 4;
    private static final int ITEM_SCHEDULE_TIME = 5;
    private static final int ITEM_SCHEDULE_DURATION = 6;
    private static final int ITEM_SCHEDULE_REPEAT = 7;
    private static final int ITEM_MICRO_BREAKS = 8;
    private static final int ITEM_NOTIFY = 9;

    private UniversalRecyclerView listView;
    private TextView statusBadge;
    private TextView statusSubtitle;
    private TextView countdownText;
    private TextView cardActionButton;
    private ImageView statusIconView;
    private View statusIconBackground;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (getContext() == null || listView == null) {
                return;
            }
            updateLiveStatusCard();
            AndroidUtilities.runOnUIThread(this, 1000);
        }
    };

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.activitySimulationUpdated);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.activitySimulationUpdated);
        AndroidUtilities.cancelRunOnUIThread(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        AndroidUtilities.runOnUIThread(timerRunnable, 1000);
        updateLiveStatusCard();
    }

    @Override
    public void onPause() {
        super.onPause();
        AndroidUtilities.cancelRunOnUIThread(timerRunnable);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.activitySimulationUpdated) {
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            updateLiveStatusCard();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ActivitySimulatorTitle", R.string.ActivitySimulatorTitle));
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

    private View createStatusCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        // Header Row with Icon and Texts
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // Circular Icon Frame
        FrameLayout iconFrame = new FrameLayout(context);
        statusIconBackground = new View(context);
        statusIconBackground.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(22), 0x1A00B0FF));
        iconFrame.addView(statusIconBackground, LayoutHelper.createFrame(44, 44));

        statusIconView = new ImageView(context);
        statusIconView.setImageResource(R.drawable.msg_usersearch);
        statusIconView.setColorFilter(new PorterDuffColorFilter(0xFF00B0FF, PorterDuff.Mode.SRC_IN));
        iconFrame.addView(statusIconView, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

        topRow.addView(iconFrame, LayoutHelper.createLinear(44, 44, 0, 0, 14, 0));

        // Text Column
        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        statusBadge = new TextView(context);
        statusBadge.setTextSize(14);
        statusBadge.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textCol.addView(statusBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        statusSubtitle = new TextView(context);
        statusSubtitle.setTextSize(13);
        statusSubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        textCol.addView(statusSubtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        topRow.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        card.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Big Countdown Text
        countdownText = new TextView(context);
        countdownText.setTextSize(26);
        countdownText.setTypeface(Typeface.DEFAULT_BOLD);
        countdownText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        countdownText.setGravity(Gravity.CENTER);
        card.addView(countdownText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 14));

        // Action Button
        cardActionButton = new TextView(context);
        cardActionButton.setTextSize(14);
        cardActionButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        cardActionButton.setGravity(Gravity.CENTER);
        cardActionButton.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        cardActionButton.setOnClickListener(v -> toggleSimulationState());
        card.addView(cardActionButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        updateLiveStatusCard();

        FrameLayout container = new FrameLayout(context);
        container.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        container.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return container;
    }

    private void updateLiveStatusCard() {
        if (statusBadge == null || countdownText == null || cardActionButton == null) {
            return;
        }

        HundredGramActivitySimulator simulator = HundredGramActivitySimulator.getInstance(currentAccount);
        boolean running = simulator.isRunning();

        if (running) {
            statusBadge.setText(LocaleController.getString("ActivitySimulatorActiveBadge", R.string.ActivitySimulatorActiveBadge));
            statusBadge.setTextColor(0xFF00C853); // vivid green
            statusIconBackground.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(22), 0x2600C853));
            statusIconView.setColorFilter(new PorterDuffColorFilter(0xFF00C853, PorterDuff.Mode.SRC_IN));

            statusSubtitle.setText(LocaleController.getString("ActivitySimulatorOnlineEmulation", R.string.ActivitySimulatorOnlineEmulation));
            countdownText.setText(simulator.getFormattedRemainingTime());
            countdownText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

            cardActionButton.setText(LocaleController.getString("ActivitySimulatorStop", R.string.ActivitySimulatorStop));
            cardActionButton.setTextColor(0xFFD32F2F);
            cardActionButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), 0x1AD32F2F));
        } else {
            statusBadge.setText(LocaleController.getString("ActivitySimulatorInactiveBadge", R.string.ActivitySimulatorInactiveBadge));
            statusBadge.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            statusIconBackground.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(22), 0x1A9E9E9E));
            statusIconView.setColorFilter(new PorterDuffColorFilter(0xFF9E9E9E, PorterDuff.Mode.SRC_IN));

            if (HundredGramConfig.activitySimulatorScheduleEnabled) {
                statusSubtitle.setText(String.format(
                        LocaleController.getString("ActivitySimulatorScheduledAt", R.string.ActivitySimulatorScheduledAt),
                        simulator.getFormattedScheduleTime(),
                        formatDuration(HundredGramConfig.activitySimulatorScheduleDurationMinutes)
                ));
                countdownText.setText(simulator.getFormattedScheduleTime());
            } else {
                statusSubtitle.setText(LocaleController.getString("ActivitySimulatorReady", R.string.ActivitySimulatorReady));
                countdownText.setText(formatDuration(HundredGramConfig.activitySimulatorQuickDurationMinutes));
            }
            countdownText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));

            cardActionButton.setText(String.format(
                    LocaleController.getString("ActivitySimulatorStartQuickBtn", R.string.ActivitySimulatorStartQuickBtn),
                    formatDuration(HundredGramConfig.activitySimulatorQuickDurationMinutes)
            ));
            cardActionButton.setTextColor(0xFFFFFFFF);
            cardActionButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), Theme.getColor(Theme.key_featuredStickers_addButton)));
        }
    }

    private void toggleSimulationState() {
        HundredGramActivitySimulator simulator = HundredGramActivitySimulator.getInstance(currentAccount);
        if (simulator.isRunning()) {
            simulator.stopSimulation(false);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_cancel,
                    LocaleController.getString("ActivitySimulatorStopped", R.string.ActivitySimulatorStopped)).show();
        } else {
            simulator.startSimulation(HundredGramConfig.activitySimulatorQuickDurationMinutes);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_usersearch,
                    LocaleController.getString("ActivitySimulatorStarted", R.string.ActivitySimulatorStarted)).show();
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        updateLiveStatusCard();
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Status Monitor Card
        items.add(UItem.asCustom(ITEM_STATUS_CARD, createStatusCard(getContext())));

        // Quick Launch Section
        items.add(UItem.asHeader(LocaleController.getString("ActivitySimulatorQuickSection", R.string.ActivitySimulatorQuickSection)));
        items.add(UItem.asButton(ITEM_QUICK_DURATION,
                LocaleController.getString("ActivitySimulatorDuration", R.string.ActivitySimulatorDuration),
                formatDuration(HundredGramConfig.activitySimulatorQuickDurationMinutes)));
        items.add(UItem.asButton(ITEM_QUICK_START, R.drawable.ic_play,
                LocaleController.getString("ActivitySimulatorStartNow", R.string.ActivitySimulatorStartNow), ""));
        items.add(UItem.asShadow(LocaleController.getString("ActivitySimulatorQuickInfo", R.string.ActivitySimulatorQuickInfo)));

        // Scheduled Session Section
        items.add(UItem.asHeader(LocaleController.getString("ActivitySimulatorScheduleSection", R.string.ActivitySimulatorScheduleSection)));
        items.add(UItem.asCheck(ITEM_SCHEDULE_ENABLED,
                LocaleController.getString("ActivitySimulatorScheduleToggle", R.string.ActivitySimulatorScheduleToggle))
                .setChecked(HundredGramConfig.activitySimulatorScheduleEnabled));

        if (HundredGramConfig.activitySimulatorScheduleEnabled) {
            HundredGramActivitySimulator simulator = HundredGramActivitySimulator.getInstance(currentAccount);
            items.add(UItem.asButton(ITEM_SCHEDULE_TIME,
                    LocaleController.getString("ActivitySimulatorScheduleTime", R.string.ActivitySimulatorScheduleTime),
                    simulator.getFormattedScheduleTime()));
            items.add(UItem.asButton(ITEM_SCHEDULE_DURATION,
                    LocaleController.getString("ActivitySimulatorScheduleDuration", R.string.ActivitySimulatorScheduleDuration),
                    formatDuration(HundredGramConfig.activitySimulatorScheduleDurationMinutes)));
            items.add(UItem.asButton(ITEM_SCHEDULE_REPEAT,
                    LocaleController.getString("ActivitySimulatorScheduleRepeat", R.string.ActivitySimulatorScheduleRepeat),
                    getRepeatName(HundredGramConfig.activitySimulatorScheduleRepeat)));
        }
        items.add(UItem.asShadow(LocaleController.getString("ActivitySimulatorScheduleInfo", R.string.ActivitySimulatorScheduleInfo)));

        // Behavior & Realism Section
        items.add(UItem.asHeader(LocaleController.getString("ActivitySimulatorBehaviorSection", R.string.ActivitySimulatorBehaviorSection)));
        items.add(UItem.asCheck(ITEM_MICRO_BREAKS,
                LocaleController.getString("ActivitySimulatorMicroBreaksToggle", R.string.ActivitySimulatorMicroBreaksToggle))
                .setChecked(HundredGramConfig.activitySimulatorMicroBreaks));
        items.add(UItem.asCheck(ITEM_NOTIFY,
                LocaleController.getString("ActivitySimulatorNotifyToggle", R.string.ActivitySimulatorNotifyToggle))
                .setChecked(HundredGramConfig.activitySimulatorNotify));
        items.add(UItem.asShadow(LocaleController.getString("ActivitySimulatorMicroBreaksInfo", R.string.ActivitySimulatorMicroBreaksInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        HundredGramActivitySimulator simulator = HundredGramActivitySimulator.getInstance(currentAccount);

        if (item.id == ITEM_QUICK_DURATION) {
            showDurationPicker(false);
        } else if (item.id == ITEM_QUICK_START) {
            toggleSimulationState();
        } else if (item.id == ITEM_SCHEDULE_ENABLED) {
            boolean newState = !HundredGramConfig.activitySimulatorScheduleEnabled;
            if (newState) {
                simulator.scheduleSession(
                        HundredGramConfig.activitySimulatorScheduleStartHour,
                        HundredGramConfig.activitySimulatorScheduleStartMinute,
                        HundredGramConfig.activitySimulatorScheduleDurationMinutes,
                        HundredGramConfig.activitySimulatorScheduleRepeat
                );
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_calendar2,
                        LocaleController.getString("ActivitySimulatorScheduleEnabledAlert", R.string.ActivitySimulatorScheduleEnabledAlert)).show();
            } else {
                simulator.cancelSchedule();
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_cancel,
                        LocaleController.getString("ActivitySimulatorScheduleDisabledAlert", R.string.ActivitySimulatorScheduleDisabledAlert)).show();
            }
            item.setChecked(newState);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(newState);
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            updateLiveStatusCard();
        } else if (item.id == ITEM_SCHEDULE_TIME) {
            showTimePicker();
        } else if (item.id == ITEM_SCHEDULE_DURATION) {
            showDurationPicker(true);
        } else if (item.id == ITEM_SCHEDULE_REPEAT) {
            showRepeatPicker();
        } else if (item.id == ITEM_MICRO_BREAKS) {
            HundredGramConfig.toggleActivitySimulatorMicroBreaks();
            item.setChecked(HundredGramConfig.activitySimulatorMicroBreaks);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HundredGramConfig.activitySimulatorMicroBreaks);
            }
        } else if (item.id == ITEM_NOTIFY) {
            HundredGramConfig.toggleActivitySimulatorNotify();
            item.setChecked(HundredGramConfig.activitySimulatorNotify);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HundredGramConfig.activitySimulatorNotify);
            }
        }
    }

    private void showTimePicker() {
        if (getParentActivity() == null) return;
        TimePickerDialog dialog = new TimePickerDialog(
                getParentActivity(),
                (view, hourOfDay, minute) -> {
                    HundredGramConfig.activitySimulatorScheduleStartHour = hourOfDay;
                    HundredGramConfig.activitySimulatorScheduleStartMinute = minute;
                    HundredGramConfig.setActivitySimulatorSchedule(
                            true,
                            hourOfDay,
                            minute,
                            HundredGramConfig.activitySimulatorScheduleDurationMinutes,
                            HundredGramConfig.activitySimulatorScheduleRepeat
                    );
                    HundredGramActivitySimulator.getInstance(currentAccount).scheduleSession(
                            hourOfDay,
                            minute,
                            HundredGramConfig.activitySimulatorScheduleDurationMinutes,
                            HundredGramConfig.activitySimulatorScheduleRepeat
                    );
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                    updateLiveStatusCard();
                },
                HundredGramConfig.activitySimulatorScheduleStartHour,
                HundredGramConfig.activitySimulatorScheduleStartMinute,
                true
        );
        showDialog(dialog);
    }

    private void showDurationPicker(boolean forSchedule) {
        if (getParentActivity() == null) return;

        int[] durations = new int[]{15, 30, 45, 60, 90, 120, 180, 240, 360, 480};
        CharSequence[] items = new CharSequence[durations.length + 1];
        for (int i = 0; i < durations.length; i++) {
            items[i] = formatDuration(durations[i]);
        }
        items[durations.length] = LocaleController.getString("ActivitySimulatorCustomDuration", R.string.ActivitySimulatorCustomDuration);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("ActivitySimulatorDuration", R.string.ActivitySimulatorDuration));
        builder.setItems(items, (dialog, which) -> {
            if (which < durations.length) {
                applyDuration(durations[which], forSchedule);
            } else {
                showCustomDurationDialog(forSchedule);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showCustomDurationDialog(boolean forSchedule) {
        if (getParentActivity() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("ActivitySimulatorCustomDuration", R.string.ActivitySimulatorCustomDuration));

        final EditText editText = new EditText(getParentActivity());
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        int current = forSchedule ? HundredGramConfig.activitySimulatorScheduleDurationMinutes : HundredGramConfig.activitySimulatorQuickDurationMinutes;
        editText.setText(String.valueOf(current));
        editText.setSelection(editText.getText().length());

        FrameLayout frame = new FrameLayout(getParentActivity());
        frame.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), 0);
        frame.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(frame);

        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            try {
                int mins = Integer.parseInt(editText.getText().toString().trim());
                if (mins > 0 && mins <= 1440) {
                    applyDuration(mins, forSchedule);
                }
            } catch (Throwable ignored) {
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void applyDuration(int durationMinutes, boolean forSchedule) {
        if (forSchedule) {
            HundredGramConfig.activitySimulatorScheduleDurationMinutes = durationMinutes;
            HundredGramConfig.setActivitySimulatorSchedule(
                    HundredGramConfig.activitySimulatorScheduleEnabled,
                    HundredGramConfig.activitySimulatorScheduleStartHour,
                    HundredGramConfig.activitySimulatorScheduleStartMinute,
                    durationMinutes,
                    HundredGramConfig.activitySimulatorScheduleRepeat
            );
        } else {
            HundredGramConfig.setActivitySimulatorQuickDuration(durationMinutes);
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        updateLiveStatusCard();
    }

    private void showRepeatPicker() {
        if (getParentActivity() == null) return;

        CharSequence[] items = new CharSequence[]{
                LocaleController.getString("ActivitySimulatorRepeatOnce", R.string.ActivitySimulatorRepeatOnce),
                LocaleController.getString("ActivitySimulatorRepeatDaily", R.string.ActivitySimulatorRepeatDaily)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("ActivitySimulatorScheduleRepeat", R.string.ActivitySimulatorScheduleRepeat));
        builder.setItems(items, (dialog, which) -> {
            HundredGramConfig.activitySimulatorScheduleRepeat = which;
            HundredGramConfig.setActivitySimulatorSchedule(
                    HundredGramConfig.activitySimulatorScheduleEnabled,
                    HundredGramConfig.activitySimulatorScheduleStartHour,
                    HundredGramConfig.activitySimulatorScheduleStartMinute,
                    HundredGramConfig.activitySimulatorScheduleDurationMinutes,
                    which
            );
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            updateLiveStatusCard();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private String getRepeatName(int repeatMode) {
        if (repeatMode == 1) {
            return LocaleController.getString("ActivitySimulatorRepeatDaily", R.string.ActivitySimulatorRepeatDaily);
        }
        return LocaleController.getString("ActivitySimulatorRepeatOnce", R.string.ActivitySimulatorRepeatOnce);
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return LocaleController.formatPluralString("Minutes", minutes);
        } else if (minutes % 60 == 0) {
            return LocaleController.formatPluralString("Hours", minutes / 60);
        } else {
            int h = minutes / 60;
            int m = minutes % 60;
            return LocaleController.formatPluralString("Hours", h) + " " + LocaleController.formatPluralString("Minutes", m);
        }
    }
}
