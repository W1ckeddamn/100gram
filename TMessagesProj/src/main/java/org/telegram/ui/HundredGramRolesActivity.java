package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.ai.HundredGramAiConfig;
import org.telegram.messenger.ai.HundredGramAiController;
import org.telegram.messenger.ai.Role;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RoleCell;
import org.telegram.ui.Components.AIEditorAlert;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HundredGramRolesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private UniversalRecyclerView listView;

    @Override
    public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this, NotificationCenter.aiRolesUpdated);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.aiRolesUpdated);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.aiRolesUpdated) {
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("Roles", R.string.Roles));
        actionBar.createMenu().addItem(0, R.drawable.msg_add);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 0) {
                    showRoleAlert(null);
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
        items.add(UItem.asTopView(LocaleController.getString("Roles", R.string.Roles), LocaleController.getString("RolesInfo", R.string.RolesInfo), "exteraGramPlaceholders", "🎭"));

        // Suggestions
        items.add(UItem.asHeader(LocaleController.getString("Suggestions", R.string.Suggestions)));
        for (final Role role : HundredGramAiController.getInstance().getSuggestedRoles()) {
            items.add(RoleCell.Factory.asRoleCell(role, v -> selectRole(role)));
        }
        items.add(UItem.asShadow(null));

        // Custom Roles
        List<Role> customRoles = HundredGramAiController.getInstance().getCustomRoles();
        if (!customRoles.isEmpty()) {
            items.add(UItem.asHeader(LocaleController.getString("Roles", R.string.Roles)));
            for (final Role role : customRoles) {
                items.add(RoleCell.Factory.asRoleCell(role, v -> selectRole(role)));
            }
            items.add(UItem.asShadow(null));
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.object instanceof Role) {
            Role role = (Role) item.object;
            if (role.isSuggestion()) {
                showRolePreview(role);
            } else {
                showRoleAlert(role);
            }
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.object instanceof Role) {
            final Role role = (Role) item.object;
            if (role.isSuggestion()) {
                return false;
            }
            ItemOptions options = ItemOptions.makeOptions(this, view);
            options.add(R.drawable.msg_edit, LocaleController.getString("Edit", R.string.Edit), () -> showRoleAlert(role));
            options.add(R.drawable.msg_copy, LocaleController.getString("Copy", R.string.Copy), () -> {
                if (AndroidUtilities.addToClipboard(role.getName() + "\n" + role.getPrompt())) {
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                }
            });
            options.add(R.drawable.msg_delete, LocaleController.getString("Delete", R.string.Delete), true, () -> confirmDeleteRole(role));
            options.setGravity(LocaleController.isRTL ? 3 : 5);
            options.show();
            return true;
        }
        return false;
    }

    private void selectRole(Role role) {
        if (role == null || role.isSelected()) return;
        HundredGramAiController.getInstance().setSelectedRole(role);
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void showRolePreview(Role role) {
        Activity parentActivity = getParentActivity();
        if (parentActivity == null) return;
        new AIEditorAlert.CreateAiStyleAlert(parentActivity, resourceProvider)
                .setLocalStylePreview(role.getName(), role.getPrompt(), role.getEmojiId(), 64, 1024)
                .show();
    }

    private void showRoleAlert(final Role role) {
        Activity parentActivity = getParentActivity();
        if (parentActivity == null) return;

        String name = role != null ? role.getName() : "";
        String prompt = role != null ? role.getPrompt() : "";
        long emojiId = role != null ? role.getEmojiId() : 0L;
        boolean isEditing = role != null && !role.isSuggestion();

        new AIEditorAlert.CreateAiStyleAlert(parentActivity, resourceProvider)
                .setLocalStyle(name, prompt, emojiId, isEditing, 64, 1024, (newName, newPrompt, newEmoji) -> {
                    Role updated = new Role(newName, newPrompt);
                    updated.setEmojiId(newEmoji != null ? newEmoji : 0L);
                    boolean isSelected = role != null && role.isSelected();

                    if (isEditing) {
                        HundredGramAiController.getInstance().updateRole(role, updated);
                    } else {
                        HundredGramAiController.getInstance().addRole(updated);
                    }

                    if (isSelected || role == null) {
                        HundredGramAiController.getInstance().setSelectedRole(updated);
                    }

                    getNotificationCenter().postNotificationName(NotificationCenter.aiRolesUpdated);
                    return true;
                })
                .show();
    }

    private void confirmDeleteRole(final Role role) {
        Activity parentActivity = getParentActivity();
        if (parentActivity == null) return;

        AlertDialog dialog = new AlertDialog.Builder(parentActivity)
                .setTitle(LocaleController.getString("Delete", R.string.Delete))
                .setMessage(AndroidUtilities.replaceTags(LocaleController.formatString("DeleteRoleInfo", R.string.DeleteRoleInfo, role.getName())))
                .setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (d, which) -> {
                    boolean wasSelected = role.isSelected();
                    if (HundredGramAiController.getInstance().removeRole(role)) {
                        if (wasSelected) {
                            HundredGramAiController.getInstance().setSelectedRole(HundredGramAiController.getInstance().getSuggestedRoles().get(0));
                        }
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .create();

        showDialog(dialog);
        View btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btn instanceof TextView) {
            ((TextView) btn).setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }
}
