package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramContentFilterController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class HundredGramContentFilterActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ITEM_ENABLE_FILTER = 1;
    private static final int ITEM_FILTER_CHANNELS = 2;
    private static final int ITEM_FILTER_GROUPS = 3;
    private static final int ITEM_FILTER_DMS = 4;
    private static final int ITEM_PRESET_CRYPTO = 5;
    private static final int ITEM_PRESET_LINKS = 6;
    private static final int ITEM_ADD_KEYWORD = 7;
    private static final int ITEM_ADD_REGEX = 8;
    private static final int ITEM_RESET_STATS = 9;

    private static final int ITEM_KEYWORD_START = 1000;
    private static final int ITEM_REGEX_START = 2000;

    private UniversalRecyclerView listView;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.updateInterfaces);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.updateInterfaces);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ContentFilterTitle", R.string.ContentFilterTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showAddRuleOptionsDialog();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(1, R.drawable.msg_add);

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Section 1: Main Toggle
        items.add(UItem.asHeader(LocaleController.getString("ContentFilterGeneral", R.string.ContentFilterGeneral)));
        items.add(UItem.asCheck(ITEM_ENABLE_FILTER, LocaleController.getString("ContentFilterEnable", R.string.ContentFilterEnable))
                .setChecked(HundredGramContentFilterController.isFilterEnabled()));
        items.add(UItem.asShadow(LocaleController.getString("ContentFilterEnableDesc", R.string.ContentFilterEnableDesc)));

        if (!HundredGramContentFilterController.isFilterEnabled()) {
            return;
        }

        // Section 2: Scope
        items.add(UItem.asHeader(LocaleController.getString("ContentFilterScope", R.string.ContentFilterScope)));
        items.add(UItem.asCheck(ITEM_FILTER_CHANNELS, LocaleController.getString("ContentFilterChannels", R.string.ContentFilterChannels))
                .setChecked(HundredGramContentFilterController.isFilterChannels()));
        items.add(UItem.asCheck(ITEM_FILTER_GROUPS, LocaleController.getString("ContentFilterGroups", R.string.ContentFilterGroups))
                .setChecked(HundredGramContentFilterController.isFilterGroups()));
        items.add(UItem.asCheck(ITEM_FILTER_DMS, LocaleController.getString("ContentFilterDMs", R.string.ContentFilterDMs))
                .setChecked(HundredGramContentFilterController.isFilterDMs()));
        items.add(UItem.asShadow(null));

        // Section 3: Presets
        items.add(UItem.asHeader(LocaleController.getString("ContentFilterPresets", R.string.ContentFilterPresets)));
        items.add(UItem.asCheck(ITEM_PRESET_CRYPTO, LocaleController.getString("ContentFilterPresetCrypto", R.string.ContentFilterPresetCrypto))
                .setChecked(HundredGramContentFilterController.isPresetCrypto()));
        items.add(UItem.asCheck(ITEM_PRESET_LINKS, LocaleController.getString("ContentFilterPresetLinks", R.string.ContentFilterPresetLinks))
                .setChecked(HundredGramContentFilterController.isPresetLinks()));
        items.add(UItem.asShadow(LocaleController.getString("ContentFilterPresetsDesc", R.string.ContentFilterPresetsDesc)));

        // Section 4: Custom Rules
        items.add(UItem.asHeader(LocaleController.getString("ContentFilterRules", R.string.ContentFilterRules)));
        items.add(UItem.asButton(ITEM_ADD_KEYWORD, R.drawable.msg_edit, LocaleController.getString("ContentFilterAddKeyword", R.string.ContentFilterAddKeyword), ""));
        items.add(UItem.asButton(ITEM_ADD_REGEX, R.drawable.msg_block2, LocaleController.getString("ContentFilterAddRegex", R.string.ContentFilterAddRegex), ""));

        List<String> keywords = HundredGramContentFilterController.getKeywords();
        List<String> regexes = HundredGramContentFilterController.getRegexPatterns();

        if (keywords.isEmpty() && regexes.isEmpty()) {
            items.add(UItem.asShadow(LocaleController.getString("ContentFilterNoRules", R.string.ContentFilterNoRules)));
        } else {
            for (int i = 0; i < keywords.size(); i++) {
                String kw = keywords.get(i);
                items.add(UItem.asButton(ITEM_KEYWORD_START + i, R.drawable.msg_edit, kw, LocaleController.getString("ContentFilterKeywordType", R.string.ContentFilterKeywordType)));
            }
            for (int i = 0; i < regexes.size(); i++) {
                String rgx = regexes.get(i);
                items.add(UItem.asButton(ITEM_REGEX_START + i, R.drawable.msg_block2, rgx, LocaleController.getString("ContentFilterRegexType", R.string.ContentFilterRegexType)));
            }
            items.add(UItem.asShadow(LocaleController.getString("ContentFilterRuleClickDesc", R.string.ContentFilterRuleClickDesc)));
        }

        // Section 5: Statistics & Reset
        int filtered = HundredGramContentFilterController.getFilteredCount();
        String statsText = LocaleController.formatPluralString("ContentFilterStatsCount", filtered, filtered);
        items.add(UItem.asHeader(LocaleController.getString("ContentFilterStats", R.string.ContentFilterStats)));
        items.add(UItem.asButton(ITEM_RESET_STATS, R.drawable.msg_delete, LocaleController.getString("ContentFilterResetStats", R.string.ContentFilterResetStats), statsText));
        items.add(UItem.asShadow(null));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_ENABLE_FILTER: {
                boolean newVal = !HundredGramContentFilterController.isFilterEnabled();
                HundredGramContentFilterController.setFilterEnabled(newVal);
                item.setChecked(newVal);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(newVal);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            }
            case ITEM_FILTER_CHANNELS: {
                boolean newVal = !HundredGramContentFilterController.isFilterChannels();
                HundredGramContentFilterController.setFilterChannels(newVal);
                item.setChecked(newVal);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(newVal);
                }
                break;
            }
            case ITEM_FILTER_GROUPS: {
                boolean newVal = !HundredGramContentFilterController.isFilterGroups();
                HundredGramContentFilterController.setFilterGroups(newVal);
                item.setChecked(newVal);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(newVal);
                }
                break;
            }
            case ITEM_FILTER_DMS: {
                boolean newVal = !HundredGramContentFilterController.isFilterDMs();
                HundredGramContentFilterController.setFilterDMs(newVal);
                item.setChecked(newVal);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(newVal);
                }
                break;
            }
            case ITEM_PRESET_CRYPTO: {
                boolean newVal = !HundredGramContentFilterController.isPresetCrypto();
                HundredGramContentFilterController.setPresetCrypto(newVal);
                item.setChecked(newVal);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(newVal);
                }
                break;
            }
            case ITEM_PRESET_LINKS: {
                boolean newVal = !HundredGramContentFilterController.isPresetLinks();
                HundredGramContentFilterController.setPresetLinks(newVal);
                item.setChecked(newVal);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(newVal);
                }
                break;
            }
            case ITEM_ADD_KEYWORD:
                showAddKeywordDialog();
                break;
            case ITEM_ADD_REGEX:
                showAddRegexDialog();
                break;
            case ITEM_RESET_STATS:
                HundredGramContentFilterController.resetFilteredCount();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("ContentFilterStatsResetToast", R.string.ContentFilterStatsResetToast)).show();
                break;
            default:
                if (item.id >= ITEM_REGEX_START) {
                    int index = item.id - ITEM_REGEX_START;
                    List<String> regexes = HundredGramContentFilterController.getRegexPatterns();
                    if (index >= 0 && index < regexes.size()) {
                        showDeleteRuleDialog(regexes.get(index), true);
                    }
                } else if (item.id >= ITEM_KEYWORD_START) {
                    int index = item.id - ITEM_KEYWORD_START;
                    List<String> keywords = HundredGramContentFilterController.getKeywords();
                    if (index >= 0 && index < keywords.size()) {
                        showDeleteRuleDialog(keywords.get(index), false);
                    }
                }
                break;
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ITEM_REGEX_START) {
            int index = item.id - ITEM_REGEX_START;
            List<String> regexes = HundredGramContentFilterController.getRegexPatterns();
            if (index >= 0 && index < regexes.size()) {
                showDeleteRuleDialog(regexes.get(index), true);
                return true;
            }
        } else if (item.id >= ITEM_KEYWORD_START) {
            int index = item.id - ITEM_KEYWORD_START;
            List<String> keywords = HundredGramContentFilterController.getKeywords();
            if (index >= 0 && index < keywords.size()) {
                showDeleteRuleDialog(keywords.get(index), false);
                return true;
            }
        }
        return false;
    }

    private void showAddRuleOptionsDialog() {
        Context context = getParentActivity();
        if (context == null) return;
        String[] options = new String[]{
                LocaleController.getString("ContentFilterAddKeyword", R.string.ContentFilterAddKeyword),
                LocaleController.getString("ContentFilterAddRegex", R.string.ContentFilterAddRegex)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("ContentFilterAddRuleTitle", R.string.ContentFilterAddRuleTitle));
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showAddKeywordDialog();
            } else {
                showAddRegexDialog();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showAddKeywordDialog() {
        Context context = getParentActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("ContentFilterAddKeywordTitle", R.string.ContentFilterAddKeywordTitle));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(8));

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setHint(LocaleController.getString("ContentFilterKeywordHint", R.string.ContentFilterKeywordHint));
        editText.setSingleLine(true);
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString("Add", R.string.Add), (dialog, which) -> {
            String text = editText.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                HundredGramContentFilterController.addKeyword(text);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showAddRegexDialog() {
        Context context = getParentActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("ContentFilterAddRegexTitle", R.string.ContentFilterAddRegexTitle));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(8));

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setHint(LocaleController.getString("ContentFilterRegexHint", R.string.ContentFilterRegexHint));
        editText.setSingleLine(true);
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton(LocaleController.getString("Add", R.string.Add), (dialog, which) -> {
            String text = editText.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                try {
                    HundredGramContentFilterController.addRegexPattern(text);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                } catch (PatternSyntaxException e) {
                    AlertDialog.Builder errBuilder = new AlertDialog.Builder(context);
                    errBuilder.setTitle(LocaleController.getString("ContentFilterRegexErrorTitle", R.string.ContentFilterRegexErrorTitle));
                    errBuilder.setMessage(e.getMessage());
                    errBuilder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                    showDialog(errBuilder.create());
                }
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showDeleteRuleDialog(String rule, boolean isRegex) {
        Context context = getParentActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("ContentFilterDeleteRuleTitle", R.string.ContentFilterDeleteRuleTitle));
        builder.setMessage(rule);
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            if (isRegex) {
                HundredGramContentFilterController.removeRegexPattern(rule);
            } else {
                HundredGramContentFilterController.removeKeyword(rule);
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }
}
