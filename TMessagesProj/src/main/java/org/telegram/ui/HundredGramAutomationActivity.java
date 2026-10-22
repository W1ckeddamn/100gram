package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramAutomationEngine;
import org.telegram.messenger.HundredGramAutomationRule;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramAutomationActivity extends BaseFragment {

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("AutomationTitle", R.string.AutomationTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showEditRuleDialog(null);
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
        items.add(UItem.asHeader(LocaleController.getString("AutomationCategory", R.string.AutomationCategory)));
        items.add(UItem.asButton(100, R.drawable.msg_add, LocaleController.getString("AutomationAddRule", R.string.AutomationAddRule), ""));
        items.add(UItem.asShadow(LocaleController.getString("AutomationSubtitle", R.string.AutomationSubtitle)));

        ArrayList<HundredGramAutomationRule> rules = HundredGramAutomationEngine.getInstance(currentAccount).getRules();
        if (rules.isEmpty()) {
            items.add(UItem.asShadow(LocaleController.getString("AutomationNoRules", R.string.AutomationNoRules)));
        } else {
            for (int i = 0; i < rules.size(); i++) {
                HundredGramAutomationRule rule = rules.get(i);
                StringBuilder sb = new StringBuilder();

                // Conditions summary
                sb.append("IF: ");
                if (!TextUtils.isEmpty(rule.keyword)) {
                    sb.append("\"").append(rule.keyword).append("\" ");
                }
                if (rule.mediaType != HundredGramAutomationRule.MEDIA_ANY) {
                    sb.append(getMediaTypeName(rule.mediaType)).append(" ");
                }
                if (TextUtils.isEmpty(rule.keyword) && rule.mediaType == HundredGramAutomationRule.MEDIA_ANY) {
                    sb.append(LocaleController.getString("AutomationMediaAny", R.string.AutomationMediaAny)).append(" ");
                }

                // Actions summary
                sb.append("• THEN: ");
                ArrayList<String> actions = new ArrayList<>();
                if (rule.saveToSaved) actions.add(LocaleController.getString("AutomationActionSaved", R.string.AutomationActionSaved));
                if (rule.markAsRead) actions.add(LocaleController.getString("AutomationActionMarkRead", R.string.AutomationActionMarkRead));
                if (rule.saveToGallery) actions.add(LocaleController.getString("AutomationActionGallery", R.string.AutomationActionGallery));
                if (rule.priorityNotify) actions.add(LocaleController.getString("AutomationActionNotify", R.string.AutomationActionNotify));
                if (rule.forwardToChatId != 0) actions.add(LocaleController.getString("AutomationActionForward", R.string.AutomationActionForward));

                if (actions.isEmpty()) {
                    sb.append("—");
                } else {
                    sb.append(TextUtils.join(", ", actions));
                }

                int itemId = 1000 + i;
                items.add(UItem.asCheck(itemId, rule.name).setChecked(rule.enabled));
                items.add(UItem.asShadow(sb.toString()));
            }
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 100) {
            showEditRuleDialog(null);
        } else if (item.id >= 1000) {
            int index = item.id - 1000;
            ArrayList<HundredGramAutomationRule> rules = HundredGramAutomationEngine.getInstance(currentAccount).getRules();
            if (index >= 0 && index < rules.size()) {
                HundredGramAutomationRule rule = rules.get(index);
                if (x > view.getWidth() - AndroidUtilities.dp(64)) {
                    // Click on checkbox toggle
                    rule.enabled = !rule.enabled;
                    HundredGramAutomationEngine.getInstance(currentAccount).toggleRule(rule.id, rule.enabled);
                    item.setChecked(rule.enabled);
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(rule.enabled);
                    }
                } else {
                    // Click on rule body opens full edit dialog
                    showEditRuleDialog(rule);
                }
            }
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 1000) {
            int index = item.id - 1000;
            ArrayList<HundredGramAutomationRule> rules = HundredGramAutomationEngine.getInstance(currentAccount).getRules();
            if (index >= 0 && index < rules.size()) {
                HundredGramAutomationRule rule = rules.get(index);
                showDeleteConfirmDialog(rule);
                return true;
            }
        }
        return false;
    }

    private void showDeleteConfirmDialog(HundredGramAutomationRule rule) {
        if (getParentActivity() == null || rule == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("Delete", R.string.Delete));
        builder.setMessage(LocaleController.formatString("AutomationDeleteConfirm", R.string.AutomationDeleteConfirm, rule.name));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            HundredGramAutomationEngine.getInstance(currentAccount).deleteRule(rule.id);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showEditRuleDialog(HundredGramAutomationRule ruleToEdit) {
        Context context = getParentActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(ruleToEdit == null ? LocaleController.getString("AutomationAddRule", R.string.AutomationAddRule) : LocaleController.getString("AutomationEditRule", R.string.AutomationEditRule));

        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        scrollView.addView(layout);

        // 1. Rule Name
        TextView nameLabel = new TextView(context);
        nameLabel.setText(LocaleController.getString("AutomationRuleName", R.string.AutomationRuleName));
        nameLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        nameLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        nameLabel.setTypeface(AndroidUtilities.bold());
        layout.addView(nameLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 4));

        EditTextBoldCursor nameEditText = new EditTextBoldCursor(context);
        nameEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nameEditText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        nameEditText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        nameEditText.setHint(LocaleController.getString("AutomationRuleName", R.string.AutomationRuleName));
        nameEditText.setSingleLine(true);
        if (ruleToEdit != null && !TextUtils.isEmpty(ruleToEdit.name)) {
            nameEditText.setText(ruleToEdit.name);
        }
        layout.addView(nameEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // 2. Conditions Header
        TextView condHeader = new TextView(context);
        condHeader.setText(LocaleController.getString("AutomationConditionHeader", R.string.AutomationConditionHeader));
        condHeader.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        condHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        condHeader.setTypeface(AndroidUtilities.bold());
        layout.addView(condHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 4));

        // Keyword
        EditTextBoldCursor keywordEditText = new EditTextBoldCursor(context);
        keywordEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        keywordEditText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        keywordEditText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        keywordEditText.setHint(LocaleController.getString("AutomationRuleKeyword", R.string.AutomationRuleKeyword));
        keywordEditText.setSingleLine(true);
        if (ruleToEdit != null && !TextUtils.isEmpty(ruleToEdit.keyword)) {
            keywordEditText.setText(ruleToEdit.keyword);
        }
        layout.addView(keywordEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        // Media Type Selector
        final int[] selectedMediaType = new int[]{ruleToEdit != null ? ruleToEdit.mediaType : HundredGramAutomationRule.MEDIA_ANY};
        TextView mediaTypeView = new TextView(context);
        mediaTypeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        mediaTypeView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        mediaTypeView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12));
        mediaTypeView.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_listSelector), AndroidUtilities.dp(6), AndroidUtilities.dp(6)));
        updateMediaTypeText(mediaTypeView, selectedMediaType[0]);
        mediaTypeView.setOnClickListener(v -> {
            AlertDialog.Builder typeBuilder = new AlertDialog.Builder(context);
            typeBuilder.setTitle(LocaleController.getString("AutomationRuleMediaType", R.string.AutomationRuleMediaType));
            String[] options = new String[]{
                    LocaleController.getString("AutomationMediaAny", R.string.AutomationMediaAny),
                    LocaleController.getString("AutomationMediaPhoto", R.string.AutomationMediaPhoto),
                    LocaleController.getString("AutomationMediaVideo", R.string.AutomationMediaVideo),
                    LocaleController.getString("AutomationMediaDocument", R.string.AutomationMediaDocument),
                    LocaleController.getString("AutomationMediaVoice", R.string.AutomationMediaVoice)
            };
            typeBuilder.setItems(options, (d, which) -> {
                selectedMediaType[0] = which;
                updateMediaTypeText(mediaTypeView, selectedMediaType[0]);
            });
            typeBuilder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(typeBuilder.create());
        });
        layout.addView(mediaTypeView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 12));

        // 3. Actions Header
        TextView actionsHeader = new TextView(context);
        actionsHeader.setText(LocaleController.getString("AutomationActionsHeader", R.string.AutomationActionsHeader));
        actionsHeader.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        actionsHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        actionsHeader.setTypeface(AndroidUtilities.bold());
        layout.addView(actionsHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 4));

        CheckBoxCell checkSaved = new CheckBoxCell(context, 1);
        checkSaved.setText(LocaleController.getString("AutomationActionSaved", R.string.AutomationActionSaved), "", ruleToEdit != null && ruleToEdit.saveToSaved, false);
        checkSaved.setOnClickListener(v -> checkSaved.setChecked(!checkSaved.isChecked(), true));
        layout.addView(checkSaved, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        CheckBoxCell checkMarkRead = new CheckBoxCell(context, 1);
        checkMarkRead.setText(LocaleController.getString("AutomationActionMarkRead", R.string.AutomationActionMarkRead), "", ruleToEdit != null && ruleToEdit.markAsRead, false);
        checkMarkRead.setOnClickListener(v -> checkMarkRead.setChecked(!checkMarkRead.isChecked(), true));
        layout.addView(checkMarkRead, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        CheckBoxCell checkGallery = new CheckBoxCell(context, 1);
        checkGallery.setText(LocaleController.getString("AutomationActionGallery", R.string.AutomationActionGallery), "", ruleToEdit != null && ruleToEdit.saveToGallery, false);
        checkGallery.setOnClickListener(v -> checkGallery.setChecked(!checkGallery.isChecked(), true));
        layout.addView(checkGallery, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        CheckBoxCell checkNotify = new CheckBoxCell(context, 1);
        checkNotify.setText(LocaleController.getString("AutomationActionNotify", R.string.AutomationActionNotify), "", ruleToEdit != null && ruleToEdit.priorityNotify, false);
        checkNotify.setOnClickListener(v -> checkNotify.setChecked(!checkNotify.isChecked(), true));
        layout.addView(checkNotify, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(scrollView);

        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            HundredGramAutomationRule rule = ruleToEdit != null ? ruleToEdit : new HundredGramAutomationRule();
            String name = nameEditText.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                name = "Правило #" + (HundredGramAutomationEngine.getInstance(currentAccount).getRules().size() + 1);
            }
            rule.name = name;
            rule.keyword = keywordEditText.getText().toString().trim();
            rule.mediaType = selectedMediaType[0];
            rule.saveToSaved = checkSaved.isChecked();
            rule.markAsRead = checkMarkRead.isChecked();
            rule.saveToGallery = checkGallery.isChecked();
            rule.priorityNotify = checkNotify.isChecked();

            if (ruleToEdit == null) {
                HundredGramAutomationEngine.getInstance(currentAccount).addRule(rule);
            } else {
                HundredGramAutomationEngine.getInstance(currentAccount).updateRule(rule);
            }

            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });

        if (ruleToEdit != null) {
            builder.setNeutralButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
                HundredGramAutomationEngine.getInstance(currentAccount).deleteRule(ruleToEdit.id);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            });
        }

        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void updateMediaTypeText(TextView textView, int mediaType) {
        String typeStr = getMediaTypeName(mediaType);
        textView.setText(LocaleController.getString("AutomationRuleMediaType", R.string.AutomationRuleMediaType) + ": " + typeStr);
    }

    private String getMediaTypeName(int mediaType) {
        switch (mediaType) {
            case HundredGramAutomationRule.MEDIA_PHOTO:
                return LocaleController.getString("AutomationMediaPhoto", R.string.AutomationMediaPhoto);
            case HundredGramAutomationRule.MEDIA_VIDEO:
                return LocaleController.getString("AutomationMediaVideo", R.string.AutomationMediaVideo);
            case HundredGramAutomationRule.MEDIA_DOCUMENT:
                return LocaleController.getString("AutomationMediaDocument", R.string.AutomationMediaDocument);
            case HundredGramAutomationRule.MEDIA_VOICE_ROUND:
                return LocaleController.getString("AutomationMediaVoice", R.string.AutomationMediaVoice);
            case HundredGramAutomationRule.MEDIA_ANY:
            default:
                return LocaleController.getString("AutomationMediaAny", R.string.AutomationMediaAny);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
