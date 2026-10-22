package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.ai.HundredGramAiConfig;
import org.telegram.messenger.ai.HundredGramAiController;
import org.telegram.messenger.ai.Service;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCheckCell2;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class HundredGramAiSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ITEM_AI_ENABLED = 1;
    private static final int ITEM_SERVICES = 2;
    private static final int ITEM_ROLES = 3;
    private static final int ITEM_HISTORY = 4;
    private static final int ITEM_REPLACE_EDITOR = 5;
    private static final int ITEM_REPLACE_SUMMARIES = 6;
    private static final int ITEM_STREAMING = 7;
    private static final int ITEM_SHOW_RESPONSE_ONLY = 8;
    private static final int ITEM_INSERT_AS_QUOTE = 9;
    private static final int ITEM_TEMPERATURE = 10;

    private UniversalRecyclerView listView;

    @Override
    public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this, NotificationCenter.aiServicesUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.aiRolesUpdated);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.aiServicesUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.aiRolesUpdated);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.aiServicesUpdated || id == NotificationCenter.aiRolesUpdated) {
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("AiSettingsTitle", R.string.AiSettingsTitle));
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
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Top View
        items.add(UItem.asTopView(
                LocaleController.getString("AiSettingsTitle", R.string.AiSettingsTitle),
                LocaleController.getString("AIChatInfo2", R.string.AIChatInfo2),
                "exteraGramPlaceholders",
                "🤖"
        ));

        // General
        items.add(UItem.asHeader(LocaleController.getString("General", R.string.General)));
        items.add(UItem.asCheck(ITEM_AI_ENABLED, LocaleController.getString("AiEnable", R.string.AiEnable))
                .setChecked(HundredGramAiConfig.isAiEnabled()));
        items.add(UItem.asShadow(LocaleController.getString("AiEnableInfo", R.string.AiEnableInfo)));

        if (HundredGramAiConfig.isAiEnabled()) {
            Service selectedService = HundredGramAiConfig.getSelectedService();
            String serviceValue = selectedService != null
                    ? (selectedService.getName() + " (" + selectedService.getShortModel() + ")")
                    : LocaleController.getString("AiKeyNotSet", R.string.AiKeyNotSet);

            String roleValue = HundredGramAiConfig.getSelectedRole();

            String historyValue;
            if (!HundredGramAiConfig.getSaveHistory()) {
                historyValue = LocaleController.getString("NotificationsOff", R.string.NotificationsOff);
            } else {
                int historyCount = HundredGramAiConfig.getConversationHistory().size();
                historyValue = historyCount == 0
                        ? LocaleController.getString("HistoryEmpty", R.string.HistoryEmpty)
                        : LocaleController.formatPluralString("messages", historyCount);
            }

            items.add(UItem.asButton(ITEM_SERVICES, R.drawable.msg_language, LocaleController.getString("Services", R.string.Services), serviceValue));
            items.add(UItem.asButton(ITEM_ROLES, R.drawable.msg_openprofile, LocaleController.getString("Roles", R.string.Roles), roleValue));
            items.add(UItem.asButton(ITEM_HISTORY, R.drawable.msg_discuss, LocaleController.getString("MessageHistory", R.string.MessageHistory), historyValue));
            items.add(UItem.asShadow(LocaleController.getString("HistoryInfo", R.string.HistoryInfo)));

            // Telegram AI Integration
            items.add(UItem.asHeader(LocaleController.getString("AIReplaceTelegram", R.string.AIReplaceTelegram)));
            items.add(UItem.asButtonCheck(ITEM_REPLACE_EDITOR, LocaleController.getString("AIFeaturesEditor", R.string.AIFeaturesEditor), LocaleController.getString("AIReplaceTelegramEditorInfo", R.string.AIReplaceTelegramEditorInfo))
                    .setChecked(HundredGramAiConfig.getReplaceTelegramEditor()));
            items.add(UItem.asButtonCheck(ITEM_REPLACE_SUMMARIES, LocaleController.getString("AIFeaturesSummaries", R.string.AIFeaturesSummaries), LocaleController.getString("AIReplaceTelegramSummariesInfo", R.string.AIReplaceTelegramSummariesInfo))
                    .setChecked(HundredGramAiConfig.getReplaceTelegramSummaries()));
            items.add(UItem.asShadow(LocaleController.getString("AIReplaceTelegramInfo", R.string.AIReplaceTelegramInfo)));

            // Generation
            items.add(UItem.asHeader(LocaleController.getString("AIGeneration", R.string.AIGeneration)));
            items.add(UItem.asButtonCheck(ITEM_STREAMING, LocaleController.getString("ResponseStreaming", R.string.ResponseStreaming), LocaleController.getString("ResponseStreamingInfo", R.string.ResponseStreamingInfo))
                    .setChecked(HundredGramAiConfig.getResponseStreaming()));
            items.add(UItem.asButtonCheck(ITEM_SHOW_RESPONSE_ONLY, LocaleController.getString("ShowResponseOnly", R.string.ShowResponseOnly), LocaleController.getString("ShowResponseOnlyInfo", R.string.ShowResponseOnlyInfo))
                    .setChecked(HundredGramAiConfig.getShowResponseOnly()));
            items.add(UItem.asButtonCheck(ITEM_INSERT_AS_QUOTE, LocaleController.getString("InsertResponseAsQuote", R.string.InsertResponseAsQuote), LocaleController.getString("InsertResponseAsQuoteInfo", R.string.InsertResponseAsQuoteInfo))
                    .setChecked(HundredGramAiConfig.getInsertAsQuote()));
            items.add(UItem.asShadow(null));

            // Temperature Slider
            items.add(UItem.asHeader(LocaleController.getString("AITemperature", R.string.AITemperature)));
            int tempInt = Math.round(HundredGramAiConfig.getTemperature() * 10.0f);
            UItem sliderItem = UItem.asIntSlideView(
                    1,
                    0,
                    tempInt,
                    20,
                    val -> String.format(Locale.US, "%.1f", val / 10.0f),
                    val -> HundredGramAiConfig.setTemperature(val / 10.0f)
            );
            sliderItem.id = ITEM_TEMPERATURE;
            sliderItem.text = LocaleController.getString("AITemperature", R.string.AITemperature);
            items.add(sliderItem);
            items.add(UItem.asShadow(LocaleController.getString("AITemperatureInfo", R.string.AITemperatureInfo)));
        }
    }

    private void setCellChecked(View view, boolean checked) {
        if (view instanceof NotificationsCheckCell) {
            ((NotificationsCheckCell) view).setChecked(checked);
        } else if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(checked);
        } else if (view instanceof TextCheckCell2) {
            ((TextCheckCell2) view).setChecked(checked);
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_AI_ENABLED:
                HundredGramAiConfig.toggleAiEnabled();
                boolean aiEnabled = HundredGramAiConfig.isAiEnabled();
                item.setChecked(aiEnabled);
                setCellChecked(view, aiEnabled);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SERVICES:
                presentFragment(new HundredGramAiServicesActivity());
                break;
            case ITEM_ROLES:
                presentFragment(new HundredGramRolesActivity());
                break;
            case ITEM_HISTORY:
                presentFragment(new HundredGramAiHistoryActivity());
                break;
            case ITEM_REPLACE_EDITOR:
                boolean replaceEditor = !HundredGramAiConfig.getReplaceTelegramEditor();
                HundredGramAiConfig.setReplaceTelegramEditor(replaceEditor);
                item.setChecked(replaceEditor);
                setCellChecked(view, replaceEditor);
                break;
            case ITEM_REPLACE_SUMMARIES:
                boolean replaceSummaries = !HundredGramAiConfig.getReplaceTelegramSummaries();
                HundredGramAiConfig.setReplaceTelegramSummaries(replaceSummaries);
                item.setChecked(replaceSummaries);
                setCellChecked(view, replaceSummaries);
                break;
            case ITEM_STREAMING:
                boolean streaming = !HundredGramAiConfig.getResponseStreaming();
                HundredGramAiConfig.setResponseStreaming(streaming);
                item.setChecked(streaming);
                setCellChecked(view, streaming);
                break;
            case ITEM_SHOW_RESPONSE_ONLY:
                boolean showOnly = !HundredGramAiConfig.getShowResponseOnly();
                HundredGramAiConfig.setShowResponseOnly(showOnly);
                item.setChecked(showOnly);
                setCellChecked(view, showOnly);
                break;
            case ITEM_INSERT_AS_QUOTE:
                boolean quote = !HundredGramAiConfig.getInsertAsQuote();
                HundredGramAiConfig.setInsertAsQuote(quote);
                item.setChecked(quote);
                setCellChecked(view, quote);
                break;
        }
    }
}
