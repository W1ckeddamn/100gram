package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.plugins.Plugin;
import org.telegram.messenger.plugins.PluginsConstants;
import org.telegram.messenger.plugins.PluginsController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HundredGramPluginsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter adapter;
    private List<Plugin> pluginList = new ArrayList<>();

    private int masterSwitchRow;
    private int safeModeRow;
    private int artOptsRow;
    private int unknownSourcesRow;
    private int sectionHeaderRow;
    private int pluginsStartRow;
    private int pluginsEndRow;
    private int emptyRow;
    private int rowCount;

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginsUpdated);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pluginMenuItemsUpdated);
        updateRows();
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pluginsUpdated);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pluginMenuItemsUpdated);
    }

    private void updateRows() {
        rowCount = 0;
        masterSwitchRow = rowCount++;
        safeModeRow = rowCount++;
        artOptsRow = rowCount++;
        unknownSourcesRow = rowCount++;
        sectionHeaderRow = rowCount++;

        pluginList.clear();
        pluginList.addAll(PluginsController.getInstance().getPlugins().values());
        Collections.sort(pluginList, new Comparator<Plugin>() {
            @Override
            public int compare(Plugin o1, Plugin o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        if (pluginList.isEmpty()) {
            pluginsStartRow = -1;
            pluginsEndRow = -1;
            emptyRow = rowCount++;
        } else {
            emptyRow = -1;
            pluginsStartRow = rowCount;
            rowCount += pluginList.size();
            pluginsEndRow = rowCount;
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.Plugins));

        ActionBarMenuItem item = actionBar.createMenu().addItem(1, R.drawable.msg_add);
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

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
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        adapter = new ListAdapter(context);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (position == masterSwitchRow) {
                    HundredGramConfig.togglePluginsEngine();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(HundredGramConfig.pluginsEngine);
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                } else if (position == safeModeRow) {
                    HundredGramConfig.togglePluginsSafeMode();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(HundredGramConfig.pluginsSafeMode);
                    }
                } else if (position == artOptsRow) {
                    HundredGramConfig.togglePluginsDisableArtOpts();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(HundredGramConfig.pluginsDisableArtOpts);
                    }
                } else if (position == unknownSourcesRow) {
                    HundredGramConfig.togglePluginsInstallUnknownSources();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(HundredGramConfig.pluginsInstallUnknownSources);
                    }
                } else if (position >= pluginsStartRow && position < pluginsEndRow) {
                    Plugin plugin = pluginList.get(position - pluginsStartRow);
                    if (PluginsController.getInstance().hasPluginSettings(plugin.getId())) {
                        presentFragment(new HundredGramPluginSettingsActivity(plugin.getId()));
                    } else {
                        BulletinFactory.of(HundredGramPluginsActivity.this).createSimpleBulletin(R.raw.info, LocaleController.formatString(R.string.PluginHasNoSettings, plugin.getName())).show();
                    }
                }
            }
        });

        listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {
            @Override
            public boolean onItemClick(View view, int position) {
                if (position >= pluginsStartRow && position < pluginsEndRow) {
                    final Plugin plugin = pluginList.get(position - pluginsStartRow);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(plugin.getName());
                    builder.setMessage(LocaleController.formatString(R.string.PluginDeleteInfo, plugin.getName()));
                    builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                        PluginsController.getInstance().deletePlugin(plugin.getId(), new Utilities.Callback<String>() {
                            @Override
                            public void run(String s) {
                                updateRows();
                                if (adapter != null) adapter.notifyDataSetChanged();
                            }
                        });
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    showDialog(builder.create());
                    return true;
                }
                return false;
            }
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 1001);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            installFromUri(uri);
        }
    }

    private void installFromUri(Uri uri) {
        try {
            String name = "plugin_" + System.currentTimeMillis();
            File cacheFile = new File(ApplicationLoader.applicationContext.getCacheDir(), name + ".plugin");
            try (InputStream in = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(cacheFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }

            PluginsController.getInstance().installPlugin(cacheFile.getAbsolutePath(), this, new Utilities.Callback<Boolean>() {
                @Override
                public void run(Boolean success) {
                    if (Boolean.TRUE.equals(success)) {
                        BulletinFactory.of(HundredGramPluginsActivity.this).createSimpleBulletin(R.raw.forward, LocaleController.getString(R.string.PluginInstalled)).show();
                        updateRows();
                        if (adapter != null) adapter.notifyDataSetChanged();
                    } else {
                        BulletinFactory.of(HundredGramPluginsActivity.this).createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.PluginInstallError)).show();
                    }
                }
            });
        } catch (Throwable e) {
            FileLog.e(e);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.PluginInstallError)).show();
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.pluginsUpdated || id == NotificationCenter.pluginMenuItemsUpdated) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    updateRows();
                    if (adapter != null) adapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        public ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            return pos == masterSwitchRow || pos == safeModeRow || pos == artOptsRow || pos == unknownSourcesRow || (pos >= pluginsStartRow && pos < pluginsEndRow);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == masterSwitchRow || position == safeModeRow || position == artOptsRow || position == unknownSourcesRow) {
                return 0; // TextCheckCell
            } else if (position == sectionHeaderRow) {
                return 1; // HeaderCell
            } else if (position == emptyRow) {
                return 2; // TextInfoPrivacyCell
            } else {
                return 3; // TextSettingsCell (plugin row)
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new TextCheckCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new HeaderCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextInfoPrivacyCell(context);
                    break;
                default:
                    view = new TextSettingsCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == masterSwitchRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.PluginsEngine), HundredGramConfig.pluginsEngine, true);
                    } else if (position == safeModeRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.PluginsSafeMode), HundredGramConfig.pluginsSafeMode, true);
                    } else if (position == artOptsRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.PluginsDisableArt), HundredGramConfig.pluginsDisableArtOpts, true);
                    } else if (position == unknownSourcesRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.PluginsInstallUnknown), HundredGramConfig.pluginsInstallUnknownSources, false);
                    }
                    break;
                case 1:
                    ((HeaderCell) holder.itemView).setText(LocaleController.getString(R.string.Plugins));
                    break;
                case 2:
                    ((TextInfoPrivacyCell) holder.itemView).setText(LocaleController.getString(R.string.PluginsInfo));
                    break;
                case 3:
                    if (position >= pluginsStartRow && position < pluginsEndRow) {
                        Plugin p = pluginList.get(position - pluginsStartRow);
                        TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                        cell.setTextAndValue(p.getName(), p.getVersion() + " (" + p.getEngine() + ")", position < pluginsEndRow - 1);
                    }
                    break;
            }
        }
    }
}
