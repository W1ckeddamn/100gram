package org.telegram.ui.Components;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.HundredGramActivityTracker;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.Calendar;

public class HundredGramUserActivityBottomSheet extends BottomSheetWithRecyclerListView implements NotificationCenter.NotificationCenterDelegate {

    private final long userId;
    private final BaseFragment fragment;
    private UniversalAdapter adapter;
    private final ArrayList<HundredGramActivityTracker.ActivityEntry> entries = new ArrayList<>();
    private boolean loaded;

    private final Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isShowing() || getContext() == null) {
                return;
            }
            if (loaded && adapter != null && HundredGramActivityTracker.getInstance(currentAccount).isSessionActive(userId)) {
                adapter.update(true);
            }
            AndroidUtilities.runOnUIThread(this, 1000);
        }
    };

    public HundredGramUserActivityBottomSheet(BaseFragment fragment, long userId) {
        super(fragment.getContext(), fragment, false, false, false, true, ActionBarType.SLIDING, fragment.getResourceProvider());
        this.fragment = fragment;
        this.userId = userId;
        topPadding = 0.35f;
        fixNavigationBar();
        setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray, resourcesProvider));
        setSlidingActionBar();
        setShowHandle(true);

        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
        loadData();
        AndroidUtilities.runOnUIThread(tickerRunnable, 1000);

        recyclerListView.setOnItemClickListener((view, position, x, y) -> {
            if (adapter == null) return;
            UItem item = adapter.getItem(position - 1);
            if (item != null && item.id == 999) {
                // Clear history button
                showClearConfirmDialog();
            } else if (item != null && !TextUtils.isEmpty(item.textValue)) {
                AndroidUtilities.addToClipboard(item.textValue);
                if (BulletinFactory.canShowBulletin(fragment)) {
                    BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                }
            }
        });
    }

    @Override
    public void dismiss() {
        AndroidUtilities.cancelRunOnUIThread(tickerRunnable);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
        super.dismiss();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            if (args != null && args.length > 0 && args[0] instanceof Integer) {
                int mask = (Integer) args[0];
                if ((mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                    loadData();
                }
            } else {
                loadData();
            }
        }
    }

    private void loadData() {
        MessagesStorage.getInstance(currentAccount).getUserActivityLogs(userId, 100, result -> {
            entries.clear();
            if (result != null) {
                entries.addAll(result);
            }
            loaded = true;
            if (adapter != null) {
                adapter.update(true);
            }
        });
    }

    private boolean isSelf() {
        return userId == org.telegram.messenger.UserConfig.getInstance(currentAccount).getClientUserId();
    }

    private void showClearConfirmDialog() {
        Context context = getContext();
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("UserActivityClear", R.string.UserActivityClear));
        builder.setMessage(LocaleController.getString(isSelf() ? "MyActivityClearConfirm" : "UserActivityClearConfirm", isSelf() ? R.string.MyActivityClearConfirm : R.string.UserActivityClearConfirm));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            HundredGramActivityTracker.getInstance(currentAccount).clearHistory(userId, () -> {
                entries.clear();
                if (adapter != null) {
                    adapter.update(true);
                }
                dismiss();
            });
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    @Override
    protected CharSequence getTitle() {
        return isSelf() ? LocaleController.getString("HundredGramMyActivityLog", R.string.HundredGramMyActivityLog) : LocaleController.getString("UserActivityLog", R.string.UserActivityLog);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(listView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        return adapter;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        boolean self = isSelf();
        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
        String name = user != null ? ContactsController.formatName(user) : "User " + userId;

        String headerTitle = self ? LocaleController.getString("HundredGramMyActivityLog", R.string.HundredGramMyActivityLog) : (name + " — " + LocaleController.getString("UserActivityLog", R.string.UserActivityLog));
        items.add(UItem.asHeader(headerTitle));

        if (!loaded) {
            items.add(UItem.asShadow(LocaleController.getString("Loading", R.string.Loading)));
            return;
        }

        if (entries.isEmpty()) {
            items.add(UItem.asShadow(LocaleController.getString("UserActivityEmpty", R.string.UserActivityEmpty)));
            return;
        }

        // Calculate today stats
        long now = System.currentTimeMillis() / 1000L;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dayStart = cal.getTimeInMillis() / 1000L;
        long todayDuration = HundredGramActivityTracker.getInstance(currentAccount).getTodayTotalOnlineTime(userId);
        int todaySessions = 0;
        boolean isActiveNow = HundredGramActivityTracker.getInstance(currentAccount).isSessionActive(userId);

        for (int a = 0; a < entries.size(); a++) {
            HundredGramActivityTracker.ActivityEntry entry = entries.get(a);
            long sessionStart = entry.timeStart;
            long sessionEnd = (a == 0 && isActiveNow) ? now : entry.timeEnd;
            if (sessionEnd <= sessionStart) {
                sessionEnd = sessionStart + 15L;
            }
            if (sessionEnd > dayStart) {
                todaySessions++;
            }
        }

        String durationStr = formatDuration(todayDuration);
        String todayTimeLabel = self ? LocaleController.getString("MyActivityTotalTodayLabel", R.string.MyActivityTotalTodayLabel) : LocaleController.getString("UserActivityTotalTodayLabel", R.string.UserActivityTotalTodayLabel);
        items.add(UItem.asButton(1, R.drawable.msg_calendar2, todayTimeLabel, durationStr));

        String todayCountLabel = LocaleController.getString("UserActivitySessionsCountLabel", R.string.UserActivitySessionsCountLabel);
        items.add(UItem.asButton(2, R.drawable.msg_usersearch, todayCountLabel, String.valueOf(todaySessions)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString("ActivityHistory", R.string.ActivityHistory)));

        for (int a = 0; a < entries.size(); a++) {
            HundredGramActivityTracker.ActivityEntry entry = entries.get(a);
            int icon;
            String triggerLabel;
            if (entry.triggerType == HundredGramActivityTracker.TRIGGER_APP_OPEN) {
                icon = R.drawable.msg_openin;
                triggerLabel = LocaleController.getString("ActivityTriggerAppOpen", R.string.ActivityTriggerAppOpen);
            } else if (entry.triggerType == HundredGramActivityTracker.TRIGGER_TYPING) {
                icon = R.drawable.msg_edit;
                triggerLabel = LocaleController.getString("ActivityTriggerTyping", R.string.ActivityTriggerTyping);
            } else if (entry.triggerType == HundredGramActivityTracker.TRIGGER_MESSAGE) {
                icon = R.drawable.msg_discussion;
                triggerLabel = LocaleController.getString("ActivityTriggerMessage", R.string.ActivityTriggerMessage);
            } else {
                icon = R.drawable.msg_usersearch;
                triggerLabel = LocaleController.getString("ActivityTriggerOnline", R.string.ActivityTriggerOnline);
            }

            String timeStartFormatted = LocaleController.formatDateTime(entry.timeStart, false);
            String title = timeStartFormatted + " • " + triggerLabel;
            String value;
            if (isActiveNow && a == 0) {
                long activeDur = Math.max(1L, now - entry.timeStart);
                value = LocaleController.getString("Online", R.string.Online) + " (" + formatSessionDuration(activeDur) + ")";
            } else {
                long dur = entry.timeEnd > entry.timeStart ? (entry.timeEnd - entry.timeStart) : 30L;
                value = formatSessionDuration(dur);
            }

            items.add(UItem.asButton(100 + a, icon, title, value));
        }

        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(999, R.drawable.msg_delete, LocaleController.getString("UserActivityClear", R.string.UserActivityClear), ""));
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0 мин";
        }
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remMinutes = minutes % 60;
        if (hours > 0) {
            return hours + " ч " + remMinutes + " мин";
        }
        return Math.max(1, minutes) + " мин";
    }

    public static String formatSessionDuration(long seconds) {
        if (seconds < 60) {
            return Math.max(1, seconds) + " сек";
        }
        return formatDuration(seconds);
    }
}
