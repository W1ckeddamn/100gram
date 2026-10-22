package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.HundredGramAccountNotifications;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramAccountNotificationsActivity extends BaseFragment {

    private UniversalRecyclerView listView;
    private float shiftDp = -4.0f;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("AccountNotifications", R.string.AccountNotifications));
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
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString("ShowNotificationsFor", R.string.ShowNotificationsFor)));
        for (int account : HundredGramAccountNotifications.getAccounts()) {
            boolean isCurrent = (account == UserConfig.selectedAccount);
            boolean checked = isCurrent || HundredGramAccountNotifications.isEnabled(account);
            items.add(UItem.asUserCheckbox(account + 1000, UserConfig.getInstance(account).getCurrentUser()).setChecked(checked));
        }
        items.add(UItem.asShadow(LocaleController.getString("AccountNotificationsInfo", R.string.AccountNotificationsInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 1000) {
            int account = item.id - 1000;
            if (account == UserConfig.selectedAccount) {
                shiftDp = -shiftDp;
                AndroidUtilities.shakeViewSpring(view, shiftDp);
                BotWebViewVibrationEffect.APP_ERROR.vibrate();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString("AccountNotificationsCurrent", R.string.AccountNotificationsCurrent)).show();
                return;
            }
            boolean newEnabled = !HundredGramAccountNotifications.isEnabled(account);
            HundredGramAccountNotifications.setEnabled(account, newEnabled);
            item.setChecked(newEnabled);
            if (view instanceof CheckBoxCell) {
                ((CheckBoxCell) view).setChecked(newEnabled, true);
            } else if (listView != null && listView.adapter != null) {
                listView.adapter.notifyItemChanged(position);
            }
        }
    }
}
