package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.ai.HundredGramAiConfig;
import org.telegram.messenger.ai.HundredGramAiController;
import org.telegram.messenger.ai.Message;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramAiHistoryActivity extends BaseFragment {

    private static final int ITEM_CLEAR_HISTORY = 1;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("MessageHistory", R.string.MessageHistory));
        actionBar.createMenu().addItem(0, R.drawable.msg_delete);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 0) {
                    HundredGramAiController.clearHistory(HundredGramAiHistoryActivity.this, () -> {
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                    });
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        listView.setSections();
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asTopView(LocaleController.getString("MessageHistory", R.string.MessageHistory), LocaleController.getString("HistoryInfo", R.string.HistoryInfo), "exteraGramPlaceholders", "💬"));

        ArrayList<Message> history = HundredGramAiConfig.getConversationHistory();
        if (history.isEmpty()) {
            items.add(UItem.asHeader(LocaleController.getString("HistoryEmpty", R.string.HistoryEmpty)));
            items.add(UItem.asShadow(null));
            return;
        }

        items.add(UItem.asHeader(LocaleController.getString("MessageHistory", R.string.MessageHistory)));
        for (int i = 0; i < history.size(); i++) {
            Message m = history.get(i);
            boolean isUser = "user".equalsIgnoreCase(m.role());
            String title = (isUser ? "👤 " : "🤖 ") + m.content();
            String roleTag = isUser ? "User" : "Assistant";
            UItem historyItem = UItem.asButton(100 + i, isUser ? R.drawable.msg_openprofile : R.drawable.msg_bot, title, roleTag);
            historyItem.object = m;
            items.add(historyItem);
        }
        items.add(UItem.asShadow(null));

        UItem clearBtn = UItem.asButton(ITEM_CLEAR_HISTORY, R.drawable.msg_delete, LocaleController.getString("ClearHistory", R.string.ClearHistory)).red();
        clearBtn.pad = 61;
        items.add(clearBtn);
        items.add(UItem.asShadow(null));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ITEM_CLEAR_HISTORY) {
            HundredGramAiController.clearHistory(this, () -> {
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            });
            return;
        }

        if (item.object instanceof Message) {
            Message m = (Message) item.object;
            showContentDialog(m.content());
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.object instanceof Message) {
            final Message m = (Message) item.object;
            ItemOptions options = ItemOptions.makeOptions(this, view);
            options.add(R.drawable.msg_copy, LocaleController.getString("Copy", R.string.Copy), () -> {
                if (AndroidUtilities.addToClipboard(m.content())) {
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                }
            });
            options.setGravity(LocaleController.isRTL ? 3 : 5);
            options.show();
            return true;
        }
        return false;
    }

    private void showContentDialog(String text) {
        Activity activity = getParentActivity();
        if (activity == null || TextUtils.isEmpty(text)) return;

        new AlertDialog.Builder(activity)
                .setTitle(LocaleController.getString("MessageHistory", R.string.MessageHistory))
                .setMessage(text)
                .setPositiveButton(LocaleController.getString("Copy", R.string.Copy), (dialog, which) -> {
                    AndroidUtilities.addToClipboard(text);
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                })
                .setNegativeButton(LocaleController.getString("Close", R.string.Close), null)
                .show();
    }
}
