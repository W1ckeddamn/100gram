package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.chats.SwipeAction;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SwipeActionsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

public class HundredGramSwipeActionsActivity extends BaseFragment {

    private static final int ID_PREVIEW = 1;
    private static final int ID_LOOP = 2;
    private static final int ID_REVERSED = 3;

    private SwipeActionsCell previewCell;
    private UniversalRecyclerView listView;
    private Drawable reorderIcon;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("SwipeActions", R.string.SwipeActions));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourceProvider));

        this.previewCell = new SwipeActionsCell(context);
        this.reorderIcon = ContextCompat.getDrawable(context, R.drawable.list_reorder);

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        listView.allowReorder(true);
        listView.listenReorder(this::onReorder);

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (previewCell != null) {
            items.add(UItem.asCustom(ID_PREVIEW, previewCell));
        }
        items.add(UItem.asShadow(LocaleController.getString("SwipeActionsInfo", R.string.SwipeActionsInfo)));

        adapter.whiteSectionStart();
        items.add(UItem.asHeader(LocaleController.getString("SwipeActionsBehavior", R.string.SwipeActionsBehavior)));
        items.add(UItem.asCheck(ID_LOOP, LocaleController.getString("SwipeActionsLoop", R.string.SwipeActionsLoop)).setChecked(HundredGramConfig.getSwipeActionsLoop()));
        items.add(UItem.asCheck(ID_REVERSED, LocaleController.getString("SwipeActionsReversed", R.string.SwipeActionsReversed)).setChecked(HundredGramConfig.getSwipeActionsReversed()));
        adapter.whiteSectionEnd();
        items.add(UItem.asShadow(LocaleController.getString("SwipeActionsBehaviorInfo", R.string.SwipeActionsBehaviorInfo)));

        List<SwipeAction> enabled = SwipeAction.enabled();
        adapter.whiteSectionStart();
        items.add(UItem.asHeader(LocaleController.getString("SwipeActionsEnabled", R.string.SwipeActionsEnabled)));
        adapter.reorderSectionStart();
        for (SwipeAction action : enabled) {
            UItem item = UItem.asButton(action.actionId + 100, action.iconRes, LocaleController.getString(action.titleRes));
            item.object2 = reorderIcon;
            items.add(item);
        }
        adapter.reorderSectionEnd();
        adapter.whiteSectionEnd();
        items.add(UItem.asShadow(LocaleController.getString("SwipeActionsOrderInfo", R.string.SwipeActionsOrderInfo)));

        List<SwipeAction> disabled = SwipeAction.disabled();
        if (!disabled.isEmpty()) {
            adapter.whiteSectionStart();
            items.add(UItem.asHeader(LocaleController.getString("SwipeActionsDisabled", R.string.SwipeActionsDisabled)));
            for (SwipeAction action : disabled) {
                items.add(UItem.asButton(action.actionId + 100, action.iconRes, LocaleController.getString(action.titleRes)));
            }
            adapter.whiteSectionEnd();
            items.add(UItem.asShadow(null));
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_LOOP) {
            HundredGramConfig.setSwipeActionsLoop(!HundredGramConfig.getSwipeActionsLoop());
            if (previewCell != null) {
                previewCell.updateActions();
            }
            refresh();
            return;
        }
        if (item.id == ID_REVERSED) {
            HundredGramConfig.setSwipeActionsReversed(!HundredGramConfig.getSwipeActionsReversed());
            refresh();
            return;
        }
        SwipeAction action = fromId(item.id);
        if (action != null) {
            action.setEnabled(!action.isEnabled());
            if (previewCell != null) {
                previewCell.updateActions();
            }
            refresh();
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void onReorder(int sectionId, ArrayList<UItem> uItems) {
        if (sectionId != 0 || uItems == null) {
            return;
        }
        ArrayList<SwipeAction> result = new ArrayList<>();
        for (UItem item : uItems) {
            SwipeAction action = fromId(item.id);
            if (action != null && !result.contains(action)) {
                result.add(action);
            }
        }
        SwipeAction.setEnabled(result);
        if (previewCell != null) {
            previewCell.updateActions();
        }
        refresh();
    }

    private SwipeAction fromId(int itemId) {
        int actionId = itemId - 100;
        return SwipeAction.of(actionId);
    }

    private void refresh() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
