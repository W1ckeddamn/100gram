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
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class HundredGramUserActivityBottomSheet extends BottomSheetWithRecyclerListView {

    private final long userId;
    private final BaseFragment fragment;
    private UniversalAdapter adapter;
    private final ArrayList<HundredGramActivityTracker.ActivityEntry> entries = new ArrayList<>();
    private boolean loaded;

    public HundredGramUserActivityBottomSheet(BaseFragment fragment, long userId) {
        super(fragment.getContext(), fragment, false, false, false, true, ActionBarType.SLIDING, fragment.getResourceProvider());
        this.fragment = fragment;
        this.userId = userId;
        topPadding = 0.35f;
        fixNavigationBar();
        setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray, resourcesProvider));
        setSlidingActionBar();
        setShowHandle(true);

        loadData();

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

    private void showClearConfirmDialog() {
        Context context = getContext();
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("UserActivityClear", R.string.UserActivityClear));
        builder.setMessage(LocaleController.getString("UserActivityClearConfirm", R.string.UserActivityClearConfirm));
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
        return LocaleController.getString("UserActivityLog", R.string.UserActivityLog);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(listView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        return adapter;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
        String name = user != null ? ContactsController.formatName(user) : "User " + userId;

        items.add(UItem.asHeader(name + " — " + LocaleController.getString("UserActivityLog", R.string.UserActivityLog)));

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
        long dayStart = now - (now % 86400);
        long todayDuration = 0;
        int todaySessions = 0;

        for (int a = 0; a < entries.size(); a++) {
            HundredGramActivityTracker.ActivityEntry entry = entries.get(a);
            if (entry.timeStart >= dayStart) {
                todaySessions++;
                todayDuration += entry.getDuration();
            }
        }

        String durationStr = formatDuration(todayDuration);
        items.add(UItem.asButton(1, R.drawable.msg_calendar2, LocaleController.formatString("UserActivityTotalToday", R.string.UserActivityTotalToday, durationStr), LocaleController.formatString("UserActivitySessionsCount", R.string.UserActivitySessionsCount, todaySessions)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString("ActivityHistory", R.string.ActivityHistory)));

        for (int a = 0; a < entries.size(); a++) {
            HundredGramActivityTracker.ActivityEntry entry = entries.get(a);
            int icon;
            String triggerLabel;
            if (entry.triggerType == HundredGramActivityTracker.TRIGGER_TYPING) {
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
            if (entry.timeEnd > entry.timeStart) {
                String dur = formatDuration(entry.getDuration());
                value = dur;
            } else if (HundredGramActivityTracker.getInstance(currentAccount).isSessionActive(userId) && a == 0) {
                value = LocaleController.getString("Online", R.string.Online);
            } else {
                value = "";
            }

            items.add(UItem.asButton(100 + a, icon, title, value));
        }

        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(999, R.drawable.msg_delete, LocaleController.getString("UserActivityClear", R.string.UserActivityClear), ""));
    }

    private static String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " сек";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " мин";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            if (minutes > 0) {
                return hours + " ч " + minutes + " мин";
            } else {
                return hours + " ч";
            }
        }
    }
}
