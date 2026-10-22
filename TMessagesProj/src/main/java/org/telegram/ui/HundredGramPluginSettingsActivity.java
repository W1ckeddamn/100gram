package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.plugins.Plugin;
import org.telegram.messenger.plugins.PluginsController;
import org.telegram.messenger.plugins.models.DividerSetting;
import org.telegram.messenger.plugins.models.HeaderSetting;
import org.telegram.messenger.plugins.models.InputSetting;
import org.telegram.messenger.plugins.models.SelectorSetting;
import org.telegram.messenger.plugins.models.SettingItem;
import org.telegram.messenger.plugins.models.SwitchSetting;
import org.telegram.messenger.plugins.models.TextSetting;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

public class HundredGramPluginSettingsActivity extends BaseFragment {

    private final String pluginId;
    private RecyclerListView listView;
    private ListAdapter adapter;
    private List<SettingItem> items = new ArrayList<>();
    private SharedPreferences preferences;

    public HundredGramPluginSettingsActivity(String pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    public boolean onFragmentCreate() {
        if (ApplicationLoader.applicationContext != null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("plugin_pref_" + pluginId, Context.MODE_PRIVATE);
        }
        loadSettings();
        return super.onFragmentCreate();
    }

    private void loadSettings() {
        items.clear();
        List<SettingItem> loaded = PluginsController.getInstance().getPluginSettingsList(pluginId);
        if (loaded != null) {
            items.addAll(loaded);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        Plugin p = PluginsController.getInstance().getPlugins().get(pluginId);
        actionBar.setTitle(p != null ? p.getName() : "Plugin Settings");

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
                if (position < 0 || position >= items.size()) return;
                SettingItem item = items.get(position);

                if (item instanceof SwitchSetting) {
                    SwitchSetting s = (SwitchSetting) item;
                    boolean cur = preferences.getBoolean(s.getKey(), s.getDefaultValue());
                    preferences.edit().putBoolean(s.getKey(), !cur).apply();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(!cur);
                    }
                    if (s.getOnChange() != null) {
                        s.getOnChange().run();
                    }
                } else if (item instanceof InputSetting) {
                    final InputSetting inp = (InputSetting) item;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(inp.getText());

                    final EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());
                    editText.setTextSize(16);
                    editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                    editText.setHintColor(Theme.getColor(Theme.key_dialogTextHint));
                    editText.setHint(inp.getHint());
                    String curVal = preferences.getString(inp.getKey(), inp.getDefaultValue());
                    editText.setText(curVal);
                    if (!TextUtils.isEmpty(curVal)) {
                        editText.setSelection(curVal.length());
                    }

                    FrameLayout container = new FrameLayout(getParentActivity());
                    container.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 10, 24, 0));
                    builder.setView(container);

                    builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
                        preferences.edit().putString(inp.getKey(), editText.getText().toString()).apply();
                        if (adapter != null) adapter.notifyDataSetChanged();
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (item instanceof SelectorSetting) {
                    final SelectorSetting sel = (SelectorSetting) item;
                    if (sel.getOptions() == null || sel.getOptions().isEmpty()) return;

                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(sel.getText());

                    CharSequence[] charSequences = sel.getOptions().toArray(new CharSequence[0]);

                    builder.setItems(charSequences, (dialog, which) -> {
                        preferences.edit().putInt(sel.getKey(), which).apply();
                        if (adapter != null) adapter.notifyDataSetChanged();
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    showDialog(builder.create());
                }
            }
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        public ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            if (pos < 0 || pos >= items.size()) return false;
            SettingItem item = items.get(pos);
            return item instanceof SwitchSetting || item instanceof InputSetting || item instanceof SelectorSetting || item instanceof TextSetting;
        }

        @Override
        public int getItemViewType(int position) {
            SettingItem item = items.get(position);
            if (item instanceof HeaderSetting) return 0;
            if (item instanceof SwitchSetting) return 1;
            if (item instanceof InputSetting) return 2;
            if (item instanceof SelectorSetting) return 3;
            if (item instanceof TextSetting) return 4;
            return 5; // divider
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextCheckCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                case 3:
                case 4:
                    view = new TextSettingsCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                default:
                    view = new ShadowSectionCell(context);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            SettingItem item = items.get(position);
            switch (holder.getItemViewType()) {
                case 0:
                    ((HeaderCell) holder.itemView).setText(((HeaderSetting) item).getText());
                    break;
                case 1:
                    SwitchSetting s = (SwitchSetting) item;
                    boolean isChecked = preferences.getBoolean(s.getKey(), s.getDefaultValue());
                    ((TextCheckCell) holder.itemView).setTextAndCheck(s.getText(), isChecked, position < items.size() - 1);
                    break;
                case 2:
                    InputSetting inp = (InputSetting) item;
                    String val = preferences.getString(inp.getKey(), inp.getDefaultValue());
                    ((TextSettingsCell) holder.itemView).setTextAndValue(inp.getText(), TextUtils.isEmpty(val) ? inp.getHint() : val, position < items.size() - 1);
                    break;
                case 3:
                    SelectorSetting sel = (SelectorSetting) item;
                    int selected = preferences.getInt(sel.getKey(), sel.getDefaultValue());
                    String optText = (sel.getOptions() != null && selected >= 0 && selected < sel.getOptions().size()) ? sel.getOptions().get(selected) : "";
                    ((TextSettingsCell) holder.itemView).setTextAndValue(sel.getText(), optText, position < items.size() - 1);
                    break;
                case 4:
                    TextSetting txt = (TextSetting) item;
                    ((TextSettingsCell) holder.itemView).setTextAndValue(txt.getText(), txt.getSubtext(), position < items.size() - 1);
                    break;
            }
        }
    }
}
