package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.translator.HundredGramTranslator;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramServicesSettingsActivity extends BaseFragment {

    private static final int ITEM_TRANSLATOR_PROVIDER = 1;
    private static final int ITEM_TRANSLATOR_TARGET_LANG = 2;
    private static final int ITEM_TRANSLATE_BEFORE_SEND = 3;
    private static final int ITEM_UNLOCK_CHAT_TRANSLATION = 4;

    private static final int ITEM_ANTISPAM_ENABLE = 10;
    private static final int ITEM_ANTISPAM_BLOCK_LINKS = 11;
    private static final int ITEM_ANTISPAM_ACTION = 12;
    private static final int ITEM_ANTISPAM_KEYWORDS = 13;
    private static final int ITEM_ANTISPAM_RESET_STATS = 14;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ServicesSettingsTitle", R.string.ServicesSettingsTitle));
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
        // === TRANSLATION SECTION ===
        items.add(UItem.asHeader(LocaleController.getString("TranslatorSection", R.string.TranslatorSection)));

        String currentProviderName = HundredGramTranslator.getInstance().getProviderName(HundredGramConfig.translationProvider);
        items.add(UItem.asButton(ITEM_TRANSLATOR_PROVIDER, R.drawable.msg_translate,
                LocaleController.getString("TranslationProvider", R.string.TranslationProvider),
                currentProviderName));

        String targetLang = HundredGramConfig.sendTargetLanguage;
        items.add(UItem.asButton(ITEM_TRANSLATOR_TARGET_LANG, R.drawable.msg_language,
                LocaleController.getString("TranslationTargetLang", R.string.TranslationTargetLang),
                targetLang != null ? targetLang.toUpperCase() : "EN"));

        items.add(UItem.asCheck(ITEM_TRANSLATE_BEFORE_SEND,
                LocaleController.getString("TranslateBeforeSend", R.string.TranslateBeforeSend))
                .setChecked(HundredGramConfig.translateBeforeSend));

        items.add(UItem.asCheck(ITEM_UNLOCK_CHAT_TRANSLATION,
                LocaleController.getString("UnlockChatTranslation", R.string.UnlockChatTranslation))
                .setChecked(HundredGramConfig.unlockChatTranslation));

        items.add(UItem.asShadow(LocaleController.getString("TranslatorSectionInfo", R.string.TranslatorSectionInfo)));

        // === ANTI-SPAM SECTION ===
        items.add(UItem.asHeader(LocaleController.getString("AntiSpamSection", R.string.AntiSpamSection)));

        items.add(UItem.asCheck(ITEM_ANTISPAM_ENABLE,
                LocaleController.getString("AntiSpamEnable", R.string.AntiSpamEnable))
                .setChecked(HundredGramConfig.antiSpamEnabled));

        if (HundredGramConfig.antiSpamEnabled) {
            items.add(UItem.asCheck(ITEM_ANTISPAM_BLOCK_LINKS,
                    LocaleController.getString("AntiSpamBlockLinks", R.string.AntiSpamBlockLinks))
                    .setChecked(HundredGramConfig.antiSpamBlockUnknownLinks));

            String actionStr;
            if (HundredGramConfig.antiSpamAction == HundredGramConfig.ANTI_SPAM_ACTION_MUTE) {
                actionStr = LocaleController.getString("AntiSpamActionMute", R.string.AntiSpamActionMute);
            } else if (HundredGramConfig.antiSpamAction == HundredGramConfig.ANTI_SPAM_ACTION_BLOCK) {
                actionStr = LocaleController.getString("AntiSpamActionBlock", R.string.AntiSpamActionBlock);
            } else {
                actionStr = LocaleController.getString("AntiSpamActionArchive", R.string.AntiSpamActionArchive);
            }

            items.add(UItem.asButton(ITEM_ANTISPAM_ACTION, R.drawable.msg_block,
                    LocaleController.getString("AntiSpamAction", R.string.AntiSpamAction),
                    actionStr));

            items.add(UItem.asButton(ITEM_ANTISPAM_KEYWORDS, R.drawable.msg_edit,
                    LocaleController.getString("AntiSpamKeywords", R.string.AntiSpamKeywords),
                    LocaleController.getString("Edit", R.string.Edit)));

            String stats = LocaleController.formatString("AntiSpamBlockedCount", R.string.AntiSpamBlockedCount, HundredGramConfig.antiSpamBlockedCount);
            items.add(UItem.asButton(ITEM_ANTISPAM_RESET_STATS, R.drawable.msg_delete,
                    stats,
                    LocaleController.getString("Reset", R.string.Reset)));
        }

        items.add(UItem.asShadow(LocaleController.getString("AntiSpamSectionInfo", R.string.AntiSpamSectionInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_TRANSLATOR_PROVIDER:
                showProviderDialog();
                break;
            case ITEM_TRANSLATOR_TARGET_LANG:
                showTargetLangDialog();
                break;
            case ITEM_TRANSLATE_BEFORE_SEND:
                HundredGramConfig.toggleTranslateBeforeSend();
                item.setChecked(HundredGramConfig.translateBeforeSend);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.translateBeforeSend);
                }
                break;
            case ITEM_UNLOCK_CHAT_TRANSLATION:
                HundredGramConfig.unlockChatTranslation = !HundredGramConfig.unlockChatTranslation;
                HundredGramConfig.save("unlockChatTranslation", HundredGramConfig.unlockChatTranslation);
                item.setChecked(HundredGramConfig.unlockChatTranslation);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.unlockChatTranslation);
                }
                break;
            case ITEM_ANTISPAM_ENABLE:
                HundredGramConfig.toggleAntiSpamEnabled();
                item.setChecked(HundredGramConfig.antiSpamEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.antiSpamEnabled);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_ANTISPAM_BLOCK_LINKS:
                HundredGramConfig.toggleAntiSpamBlockUnknownLinks();
                item.setChecked(HundredGramConfig.antiSpamBlockUnknownLinks);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.antiSpamBlockUnknownLinks);
                }
                break;
            case ITEM_ANTISPAM_ACTION:
                showAntiSpamActionDialog();
                break;
            case ITEM_ANTISPAM_KEYWORDS:
                showKeywordsDialog();
                break;
            case ITEM_ANTISPAM_RESET_STATS:
                HundredGramConfig.resetAntiSpamBlockedCount();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                if (BulletinFactory.canShowBulletin(this)) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.done, LocaleController.getString("HundredGramSettingsSaved", R.string.HundredGramSettingsSaved)).show();
                }
                break;
        }
    }

    private void showProviderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("TranslationProvider", R.string.TranslationProvider));

        CharSequence[] options = new CharSequence[]{
                "Telegram (Official)",
                "Google Translate",
                "Yandex Translate",
                "DeepL"
        };

        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setTranslationProvider(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });

        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTargetLangDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("TranslationTargetLang", R.string.TranslationTargetLang));

        CharSequence[] langs = new CharSequence[]{"en", "ru", "de", "es", "fr", "it", "zh", "tr", "ar", "ja", "ko", "uk", "pl"};
        CharSequence[] names = new CharSequence[]{
                "English (EN)",
                "Русский (RU)",
                "Deutsch (DE)",
                "Español (ES)",
                "Français (FR)",
                "Italiano (IT)",
                "中文 (ZH)",
                "Türkçe (TR)",
                "العربية (AR)",
                "日本語 (JA)",
                "한국어 (KO)",
                "Українська (UK)",
                "Polski (PL)"
        };

        builder.setItems(names, (dialog, which) -> {
            HundredGramConfig.setSendTargetLanguage(langs[which].toString());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });

        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showAntiSpamActionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("AntiSpamAction", R.string.AntiSpamAction));

        CharSequence[] options = new CharSequence[]{
                LocaleController.getString("AntiSpamActionMute", R.string.AntiSpamActionMute),
                LocaleController.getString("AntiSpamActionArchive", R.string.AntiSpamActionArchive),
                LocaleController.getString("AntiSpamActionBlock", R.string.AntiSpamActionBlock)
        };

        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setAntiSpamAction(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });

        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showKeywordsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("AntiSpamKeywords", R.string.AntiSpamKeywords));

        final EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(HundredGramConfig.antiSpamKeywords != null ? HundredGramConfig.antiSpamKeywords : "");
        builder.setView(input);

        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String text = input.getText() != null ? input.getText().toString().trim() : "";
            HundredGramConfig.setAntiSpamKeywords(text);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            if (BulletinFactory.canShowBulletin(this)) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, LocaleController.getString("HundredGramSettingsSaved", R.string.HundredGramSettingsSaved)).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }
}
