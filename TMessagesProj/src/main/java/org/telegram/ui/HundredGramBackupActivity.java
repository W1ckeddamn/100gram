package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramBackupManager;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * Экран управления резервным копированием и восстановлением настроек 100gram.
 */
public class HundredGramBackupActivity extends BaseFragment {

    private static final int ITEM_EXPORT_SETTINGS = 1;
    private static final int ITEM_IMPORT_SETTINGS = 2;
    private static final int ITEM_RESET_SETTINGS = 3;

    private static final int REQUEST_CODE_PICK_BACKUP = 6012;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("HundredGramBackupTitle", R.string.HundredGramBackupTitle));
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

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString("HundredGramBackupTitle", R.string.HundredGramBackupTitle)));

        items.add(UItem.asButton(ITEM_EXPORT_SETTINGS, R.drawable.msg_download,
                LocaleController.getString("HundredGramExportSettings", R.string.HundredGramExportSettings),
                LocaleController.getString("HundredGramExportSettingsSub", R.string.HundredGramExportSettingsSub)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asButton(ITEM_IMPORT_SETTINGS, R.drawable.msg_archive,
                LocaleController.getString("HundredGramImportSettings", R.string.HundredGramImportSettings),
                LocaleController.getString("HundredGramImportSettingsSub", R.string.HundredGramImportSettingsSub)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString("Reset", R.string.Reset)));

        UItem resetItem = UItem.asButton(ITEM_RESET_SETTINGS, R.drawable.msg_reset,
                LocaleController.getString("HundredGramResetSettings", R.string.HundredGramResetSettings),
                LocaleController.getString("HundredGramResetSettingsSub", R.string.HundredGramResetSettingsSub));
        resetItem.red = true;
        items.add(resetItem);
        items.add(UItem.asShadow(null));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ITEM_EXPORT_SETTINGS) {
            HundredGramBackupManager.shareBackupFile(this);
            return;
        }

        if (item.id == ITEM_IMPORT_SETTINGS) {
            openFilePicker();
            return;
        }

        if (item.id == ITEM_RESET_SETTINGS) {
            showResetConfirmDialog();
        }
    }

    private void showResetConfirmDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramResetSettings", R.string.HundredGramResetSettings));
        builder.setMessage(LocaleController.getString("HundredGramResetSettingsConfirm", R.string.HundredGramResetSettingsConfirm));
        builder.setPositiveButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
            HundredGramBackupManager.resetAllSettings(getParentActivity());
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_reset,
                    LocaleController.getString("HundredGramSettingsResetDone", R.string.HundredGramSettingsResetDone)).show();
            if (parentLayout != null) {
                parentLayout.rebuildFragments(0);
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
            startActivityForResult(intent, REQUEST_CODE_PICK_BACKUP);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_BACKUP && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            boolean success = HundredGramBackupManager.importSettingsFromUri(getParentActivity(), uri);
            if (success) {
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_theme,
                        LocaleController.getString("HundredGramImportSuccess", R.string.HundredGramImportSuccess)).show();
                if (parentLayout != null) {
                    parentLayout.rebuildFragments(0);
                }
            } else {
                BulletinFactory.of(this).createErrorBulletin(
                        LocaleController.getString("HundredGramImportFailed", R.string.HundredGramImportFailed)).show();
            }
        }
    }
}
