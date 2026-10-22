package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.icons.HundredGramIconManager;
import org.telegram.messenger.icons.HundredGramIconPack;
import org.telegram.messenger.icons.HundredGramIconPackStorage;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Экран управления пакетами иконок форка 100gram.
 */
public class HundredGramIconPacksActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ITEM_ENABLE_ICON_PACKS = 1;
    private static final int ITEM_INSTALL_PACK = 2;
    private static final int BASE_PACK_ITEM_ID = 100;

    private static final int REQUEST_CODE_PICK_PACK = 5123;

    private UniversalRecyclerView listView;

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didUpdateIconPack);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didUpdateIconPack);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("HundredGramIconPacks", R.string.HundredGramIconPacks));
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

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString("Settings", R.string.Settings)));
        UItem checkItem = UItem.asCheck(ITEM_ENABLE_ICON_PACKS,
                LocaleController.getString("HundredGramIconPacksEnable", R.string.HundredGramIconPacksEnable));
        checkItem.checked = HundredGramConfig.iconPacksEnabled;
        items.add(checkItem);
        items.add(UItem.asShadow(LocaleController.getString("HundredGramIconPacksEnableSub", R.string.HundredGramIconPacksEnableSub)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramIconPacksHeader", R.string.HundredGramIconPacksHeader)));

        List<HundredGramIconPack> packs = HundredGramIconManager.getInstance().getInstalledPacks();
        String currentSelectedId = HundredGramConfig.selectedIconPack;

        for (int i = 0; i < packs.size(); i++) {
            HundredGramIconPack pack = packs.get(i);
            boolean isChecked = pack.getId().equals(currentSelectedId);
            String desc = pack.getAuthor();
            if (pack.isDefault()) {
                desc = "Стандартный системный стиль Telegram";
            } else {
                desc = pack.getAuthor() + " • v" + pack.getVersion() + " • " + pack.getIconCount() + " иконок";
            }

            int iconRes = pack.isDefault() ? R.drawable.msg_theme : R.drawable.msg_sticker;
            UItem uItem = UItem.asRadio(BASE_PACK_ITEM_ID + i, pack.getName());
            uItem.checked = isChecked;
            uItem.subtext = desc;
            uItem.iconResId = iconRes;
            items.add(uItem);
        }

        items.add(UItem.asShadow(null));

        items.add(UItem.asButton(ITEM_INSTALL_PACK, R.drawable.msg_add,
                LocaleController.getString("HundredGramInstallIconPack", R.string.HundredGramInstallIconPack),
                LocaleController.getString("HundredGramInstallIconPackSub", R.string.HundredGramInstallIconPackSub)));
        items.add(UItem.asShadow(null));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ITEM_ENABLE_ICON_PACKS) {
            HundredGramConfig.toggleIconPacksEnabled();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            if (parentLayout != null) {
                parentLayout.rebuildFragments(0);
            }
            return;
        }

        if (item.id == ITEM_INSTALL_PACK) {
            openFilePicker();
            return;
        }

        if (item.id >= BASE_PACK_ITEM_ID) {
            int index = item.id - BASE_PACK_ITEM_ID;
            List<HundredGramIconPack> packs = HundredGramIconManager.getInstance().getInstalledPacks();
            if (index >= 0 && index < packs.size()) {
                HundredGramIconPack pack = packs.get(index);
                HundredGramIconManager.getInstance().setActivePack(pack.getId());
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                if (parentLayout != null) {
                    parentLayout.rebuildFragments(0);
                }
            }
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= BASE_PACK_ITEM_ID) {
            int index = item.id - BASE_PACK_ITEM_ID;
            List<HundredGramIconPack> packs = HundredGramIconManager.getInstance().getInstalledPacks();
            if (index >= 0 && index < packs.size()) {
                HundredGramIconPack pack = packs.get(index);
                if (!pack.isDefault()) {
                    showDeletePackDialog(pack);
                    return true;
                }
            }
        }
        return false;
    }

    private void showDeletePackDialog(HundredGramIconPack pack) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramIconPacks", R.string.HundredGramIconPacks));
        builder.setMessage(String.format(LocaleController.getString("HundredGramIconPackDeleteConfirm", R.string.HundredGramIconPackDeleteConfirm), pack.getName()));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            boolean deleted = HundredGramIconPackStorage.deletePack(pack.getId());
            if (deleted) {
                if (pack.getId().equals(HundredGramConfig.selectedIconPack)) {
                    HundredGramIconManager.getInstance().setActivePack("default");
                }
                HundredGramIconManager.getInstance().reloadInstalledPacks();
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete,
                        LocaleController.getString("HundredGramIconPackDeleted", R.string.HundredGramIconPackDeleted)).show();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                if (parentLayout != null) {
                    parentLayout.rebuildFragments(0);
                }
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE_PICK_PACK);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_PACK && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            processPickedArchive(uri);
        }
    }

    private void processPickedArchive(Uri uri) {
        if (uri == null || getParentActivity() == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            try (InputStream is = getParentActivity().getContentResolver().openInputStream(uri)) {
                HundredGramIconPack pack = HundredGramIconPackStorage.installPackFromZip(is, "pack_" + System.currentTimeMillis());
                if (pack != null) {
                    HundredGramIconManager.getInstance().reloadInstalledPacks();
                    HundredGramIconManager.getInstance().setActivePack(pack.getId());
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme,
                            String.format(LocaleController.getString("HundredGramIconPackInstalled", R.string.HundredGramIconPackInstalled), pack.getName())).show();
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                    if (parentLayout != null) {
                        parentLayout.rebuildFragments(0);
                    }
                } else {
                    BulletinFactory.of(this).createErrorBulletin(
                            LocaleController.getString("HundredGramIconPackInstallFailed", R.string.HundredGramIconPackInstallFailed)).show();
                }
            } catch (Exception e) {
                FileLog.e(e);
                BulletinFactory.of(this).createErrorBulletin(
                        LocaleController.getString("HundredGramIconPackInstallFailed", R.string.HundredGramIconPackInstallFailed)).show();
            }
        });
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didUpdateIconPack) {
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }
}
