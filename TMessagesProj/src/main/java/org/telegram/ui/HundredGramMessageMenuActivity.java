package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.HundredGramMessageMenu;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramMessageMenuActivity extends BaseFragment {

    private static final int ITEM_RESET = 1;

    private static final int[] GROUP_TITLES = {
            R.string.MessageMenuGroupReply,
            R.string.MessageMenuGroupCopy,
            R.string.MessageMenuGroupManage,
            R.string.MessageMenuGroupAi,
            R.string.MessageMenuGroupSave,
            R.string.MessageMenuGroupOther
    };

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.MessageMenuCustomize));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == ITEM_RESET) {
                    HundredGramMessageMenu.reset();
                    refresh();
                }
            }
        });

        ActionBarMenuItem otherItem = actionBar.createMenu().addItem(0, R.drawable.ic_ab_other);
        otherItem.addSubItem(ITEM_RESET, R.drawable.msg_reset, LocaleController.getString(R.string.MessageMenuReset));

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourceProvider));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        listView.allowReorder(true);
        listView.listenReorder(this::onReorder);

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow(LocaleController.getString(R.string.MessageMenuInfo)));
        for (int group = 0; group < HundredGramMessageMenu.GROUP_COUNT; group++) {
            items.add(UItem.asHeader(LocaleController.getString(GROUP_TITLES[group])));
            adapter.reorderSectionStart();
            for (HundredGramMessageMenu.Entry entry : HundredGramMessageMenu.entries()) {
                if (entry.group != group) {
                    continue;
                }
                items.add(UItem.asButtonCheck(HundredGramMessageMenu.idOf(entry.key), LocaleController.getString(entry.labelRes), null)
                        .setChecked(!HundredGramMessageMenu.isHidden(entry.key)));
            }
            adapter.reorderSectionEnd();
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        String key = HundredGramMessageMenu.keyOfId(item.id);
        if (key == null) {
            return;
        }
        HundredGramMessageMenu.setHidden(key, !HundredGramMessageMenu.isHidden(key));
        refresh();
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void onReorder(int sectionId, ArrayList<UItem> uItems) {
        if (uItems == null) {
            return;
        }
        ArrayList<String> keys = new ArrayList<>(uItems.size());
        for (UItem item : uItems) {
            String key = HundredGramMessageMenu.keyOfId(item.id);
            if (key != null && !keys.contains(key)) {
                keys.add(key);
            }
        }
        HundredGramMessageMenu.applySectionOrder(keys);
        refresh();
    }

    private void refresh() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
