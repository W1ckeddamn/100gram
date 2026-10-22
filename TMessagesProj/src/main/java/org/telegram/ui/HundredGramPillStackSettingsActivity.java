package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PillStack.PillRegistry;
import org.telegram.ui.Components.PillStack.PillStackConfig;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран настройки интерактивных плашек поиска (PillStack).
 */
public class HundredGramPillStackSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ITEM_ENABLE_PILL_STACK = 1;
    private static final int ITEM_INFINITE_SCROLLING = 2;

    private static final int BASE_ACTIVE_PILL = 100;
    private static final int BASE_HIDDEN_PILL = 200;
    private static final int MENU_RESET = 300;

    private UniversalRecyclerView listView;

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pillStackSettingsChanged);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pillStackSettingsChanged);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("PillStackPills", R.string.PillStackPills));

        ActionBarMenuItem resetItem = actionBar.createMenu().addItem(MENU_RESET, R.drawable.msg_reset);
        resetItem.setContentDescription(LocaleController.getString(R.string.Reset));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_RESET) {
                    confirmReset();
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

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString("Settings", R.string.Settings)));

        UItem checkPillStack = UItem.asCheck(ITEM_ENABLE_PILL_STACK,
                LocaleController.getString("HundredGramPillStack", R.string.HundredGramPillStack));
        checkPillStack.checked = HundredGramConfig.pillStackEnabled;
        items.add(checkPillStack);
        items.add(UItem.asShadow(LocaleController.getString("HundredGramPillStackInfo", R.string.HundredGramPillStackInfo)));

        if (HundredGramConfig.pillStackEnabled) {
            items.add(UItem.asHeader(LocaleController.getString("General", R.string.General)));

            UItem checkInfinite = UItem.asCheck(ITEM_INFINITE_SCROLLING,
                    LocaleController.getString("PillStackInfiniteScrolling", R.string.PillStackInfiniteScrolling));
            checkInfinite.checked = PillStackConfig.getInfiniteScrolling();
            items.add(checkInfinite);
            items.add(UItem.asShadow(LocaleController.getString("PillStackInfiniteScrollingSub", R.string.PillStackInfiniteScrollingSub)));

            List<Integer> activeList = PillStackConfig.getActivePills();
            if (!activeList.isEmpty()) {
                items.add(UItem.asHeader(LocaleController.getString("PillStackActivePills", R.string.PillStackActivePills)));
                for (int i = 0; i < activeList.size(); i++) {
                    int id = activeList.get(i);
                    PillRegistry.PillInfo info = PillRegistry.getPillInfo(id);
                    if (info != null) {
                        UItem item = UItem.asButton(BASE_ACTIVE_PILL + id, info.iconRes, info.name,
                                LocaleController.getString("PillStackTapToHide", R.string.PillStackTapToHide));
                        items.add(item);
                    }
                }
                items.add(UItem.asShadow(null));
            }

            List<Integer> hiddenList = PillStackConfig.getHiddenPills();
            if (!hiddenList.isEmpty()) {
                items.add(UItem.asHeader(LocaleController.getString("PillStackHiddenPills", R.string.PillStackHiddenPills)));
                for (int i = 0; i < hiddenList.size(); i++) {
                    int id = hiddenList.get(i);
                    PillRegistry.PillInfo info = PillRegistry.getPillInfo(id);
                    if (info != null) {
                        UItem item = UItem.asButton(BASE_HIDDEN_PILL + id, info.iconRes, info.name,
                                LocaleController.getString("PillStackTapToShow", R.string.PillStackTapToShow));
                        items.add(item);
                    }
                }
                items.add(UItem.asShadow(LocaleController.getString("PillStackPillsSettingsInfo", R.string.PillStackPillsSettingsInfo)));
            }
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ITEM_ENABLE_PILL_STACK) {
            HundredGramConfig.togglePillStackEnabled();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (item.id == ITEM_INFINITE_SCROLLING) {
            boolean current = PillStackConfig.getInfiniteScrolling();
            PillStackConfig.setInfiniteScrolling(!current);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (item.id >= BASE_ACTIVE_PILL && item.id < BASE_HIDDEN_PILL) {
            // Tap on active pill -> hide it
            int pillId = item.id - BASE_ACTIVE_PILL;
            List<Integer> active = PillStackConfig.getActivePills();
            List<Integer> hidden = PillStackConfig.getHiddenPills();
            if (active.remove((Integer) pillId)) {
                if (!hidden.contains(pillId)) {
                    hidden.add(pillId);
                }
                PillStackConfig.savePillsLayout(active, hidden);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }
        } else if (item.id >= BASE_HIDDEN_PILL && item.id < MENU_RESET) {
            // Tap on hidden pill -> activate it
            int pillId = item.id - BASE_HIDDEN_PILL;
            List<Integer> active = PillStackConfig.getActivePills();
            List<Integer> hidden = PillStackConfig.getHiddenPills();
            if (hidden.remove((Integer) pillId)) {
                if (!active.contains(pillId)) {
                    active.add(pillId);
                }
                PillStackConfig.savePillsLayout(active, hidden);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }
        }
    }

    private void confirmReset() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("ResetSettings", R.string.ResetSettings));
        builder.setMessage(LocaleController.getString("PillStackResetConfirm", R.string.PillStackResetConfirm));
        builder.setPositiveButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
            PillStackConfig.resetToDefault();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, LocaleController.getString("PillStackResetDone", R.string.PillStackResetDone)).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.pillStackSettingsChanged) {
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }
}
