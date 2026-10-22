package org.telegram.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.ai.HundredGramAiClient;
import org.telegram.messenger.ai.HundredGramAiConfig;
import org.telegram.messenger.ai.Service;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.EditTextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramEditServiceActivity extends BaseFragment {

    private static class ServicePreset {
        final String name;
        final String url;
        final String model;

        ServicePreset(String name, String url, String model) {
            this.name = name;
            this.url = url;
            this.model = model;
        }
    }

    private static final ServicePreset[] PRESETS = new ServicePreset[] {
            new ServicePreset("Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "gemini-2.5-flash"),
            new ServicePreset("OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-4o-mini"),
            new ServicePreset("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "openai/gpt-4o-mini"),
            new ServicePreset("Groq", "https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile"),
            new ServicePreset("Perplexity", "https://api.perplexity.ai/chat/completions", "sonar"),
            new ServicePreset(null, null, null) // Custom
    };

    private static final int ITEM_PRESET_BASE = 10;
    private static final int ITEM_PASTE_CLIPBOARD = 1;
    private static final int ITEM_REASONING = 2;
    private static final int ITEM_TEST_SAVE = 3;

    private final Service currentService;
    private int selectedPresetIndex = -1;
    private boolean reasoningEnabled;

    private EditTextCell nameCell;
    private EditTextCell urlCell;
    private EditTextCell modelCell;
    private EditTextCell keyCell;

    private UniversalRecyclerView listView;
    private AlertDialog progressDialog;
    private HundredGramAiClient.Cancellable testRequest;

    public HundredGramEditServiceActivity(Service service) {
        this.currentService = service;
        if (service != null) {
            this.reasoningEnabled = service.isReasoningEnabled();
            this.selectedPresetIndex = findPresetIndex(service.getUrl(), service.getModel());
        } else {
            this.selectedPresetIndex = 0; // Default to Gemini
        }
    }

    private int findPresetIndex(String url, String model) {
        if (url == null) return PRESETS.length - 1;
        for (int i = 0; i < PRESETS.length - 1; i++) {
            if (url.equalsIgnoreCase(PRESETS[i].url)) {
                return i;
            }
        }
        return PRESETS.length - 1;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(currentService != null ? "EditService" : "NewService", currentService != null ? R.string.EditService : R.string.NewService));
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

        createFields(context);

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, null);
        listView.setSections();
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void createFields(Context context) {
        nameCell = new EditTextCell(context, LocaleController.getString("ServiceName", R.string.ServiceName), false, false, 64, resourceProvider);
        nameCell.editText.setHint(LocaleController.getString("ServiceName", R.string.ServiceName));

        urlCell = new EditTextCell(context, LocaleController.getString("ServiceUrl", R.string.ServiceUrl), false, false, 256, resourceProvider);
        urlCell.editText.setHint("https://api.openai.com/v1/chat/completions");

        modelCell = new EditTextCell(context, LocaleController.getString("ServiceModel", R.string.ServiceModel), false, false, 64, resourceProvider);
        modelCell.editText.setHint("gpt-4o-mini");

        keyCell = new EditTextCell(context, LocaleController.getString("ServiceKey", R.string.ServiceKey), false, false, 256, resourceProvider);
        keyCell.editText.setHint("sk-…");

        if (currentService != null) {
            nameCell.setText(currentService.getName());
            urlCell.setText(currentService.getUrl());
            modelCell.setText(currentService.getModel());
            keyCell.setText(currentService.getKey());
        } else if (selectedPresetIndex >= 0 && selectedPresetIndex < PRESETS.length - 1) {
            ServicePreset p = PRESETS[selectedPresetIndex];
            nameCell.setText(p.name);
            urlCell.setText(p.url);
            modelCell.setText(p.model);
        }
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString("ServicePreset", R.string.ServicePreset)));
        for (int i = 0; i < PRESETS.length; i++) {
            String title = (i < PRESETS.length - 1) ? PRESETS[i].name : LocaleController.getString("ServicePresetCustom", R.string.ServicePresetCustom);
            UItem radioItem = UItem.asRadio(ITEM_PRESET_BASE + i, title).setChecked(selectedPresetIndex == i);
            items.add(radioItem);
        }
        items.add(UItem.asShadow(null));

        items.add(UItem.asButton(ITEM_PASTE_CLIPBOARD, R.drawable.msg_copy, LocaleController.getString("PasteFromClipboard", R.string.PasteFromClipboard)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString("Settings", R.string.Settings)));
        items.add(UItem.asCustom(nameCell));
        items.add(UItem.asCustom(urlCell));
        items.add(UItem.asCustom(modelCell));
        items.add(UItem.asCustom(keyCell));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString("AIGeneration", R.string.AIGeneration)));
        items.add(UItem.asCheck(ITEM_REASONING, LocaleController.getString("ServiceReasoning", R.string.ServiceReasoning))
                .setChecked(reasoningEnabled));
        items.add(UItem.asShadow(LocaleController.getString("ServiceReasoningInfo", R.string.ServiceReasoningInfo)));

        UItem testSaveBtn = UItem.asButton(ITEM_TEST_SAVE, LocaleController.getString("TestAndSave", R.string.TestAndSave)).accent();
        testSaveBtn.pad = 61;
        items.add(testSaveBtn);
        items.add(UItem.asShadow(null));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ITEM_PRESET_BASE && item.id < ITEM_PRESET_BASE + PRESETS.length) {
            int newIndex = item.id - ITEM_PRESET_BASE;
            selectedPresetIndex = newIndex;
            if (newIndex < PRESETS.length - 1) {
                ServicePreset p = PRESETS[newIndex];
                if (nameCell != null) nameCell.setText(p.name);
                if (urlCell != null) urlCell.setText(p.url);
                if (modelCell != null) modelCell.setText(p.model);
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            return;
        }

        if (item.id == ITEM_PASTE_CLIPBOARD) {
            pasteFromClipboard();
            return;
        }

        if (item.id == ITEM_REASONING) {
            reasoningEnabled = !reasoningEnabled;
            item.setChecked(reasoningEnabled);
            if (view instanceof org.telegram.ui.Cells.NotificationsCheckCell) {
                ((org.telegram.ui.Cells.NotificationsCheckCell) view).setChecked(reasoningEnabled);
            } else if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(reasoningEnabled);
            }
            return;
        }

        if (item.id == ITEM_TEST_SAVE) {
            testAndSave();
        }
    }

    private void pasteFromClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData clip = cm.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (!TextUtils.isEmpty(text)) {
                        String str = text.toString().trim();
                        if (str.startsWith("http://") || str.startsWith("https://")) {
                            urlCell.setText(str);
                            selectedPresetIndex = PRESETS.length - 1;
                            if (listView != null && listView.adapter != null) listView.adapter.update(true);
                            BulletinFactory.of(this).createSimpleBulletin(R.raw.copy, "URL pasted").show();
                        } else {
                            keyCell.setText(str);
                            BulletinFactory.of(this).createSimpleBulletin(R.raw.copy, "API Key pasted").show();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void testAndSave() {
        String url = urlCell.getText().toString().trim();
        String model = modelCell.getText().toString().trim();
        String key = keyCell.getText().toString().trim();
        String name = nameCell.getText().toString().trim();

        if (TextUtils.isEmpty(url)) {
            AndroidUtilities.shakeView(urlCell);
            return;
        }
        if (TextUtils.isEmpty(model)) {
            AndroidUtilities.shakeView(modelCell);
            return;
        }
        if (TextUtils.isEmpty(key)) {
            AndroidUtilities.shakeView(keyCell);
            return;
        }
        if (TextUtils.isEmpty(name)) {
            name = model;
        }

        final Service candidate = new Service(
                currentService != null ? currentService.getId() : null,
                name,
                url,
                model,
                key,
                reasoningEnabled
        );

        Activity activity = getParentActivity();
        if (activity == null) return;

        progressDialog = new AlertDialog(activity, 3);
        progressDialog.setMessage(LocaleController.getString("TestingService", R.string.TestingService));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(d -> {
            if (testRequest != null) {
                testRequest.cancel();
            }
        });
        progressDialog.show();

        // Perform test connection
        Service prevSelected = HundredGramAiConfig.getSelectedService();
        HundredGramAiConfig.setSelectedService(candidate);

        testRequest = HundredGramAiClient.request("Respond with OK", null, false, new HundredGramAiClient.GenerationCallback() {
            @Override
            public void onChunk(String accumulatedText, String newDelta) {}

            @Override
            public void onResponse(String fullText) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (progressDialog != null) {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception ignored) {}
                    }

                    HundredGramAiConfig.addOrUpdateService(candidate);
                    HundredGramAiConfig.setSelectedService(candidate);
                    getNotificationCenter().postNotificationName(NotificationCenter.aiServicesUpdated);

                    BulletinFactory.of(HundredGramEditServiceActivity.this)
                            .createSimpleBulletin(R.raw.contact_check, LocaleController.getString("TestSuccess", R.string.TestSuccess))
                            .show();

                    AndroidUtilities.runOnUIThread(() -> finishFragment(), 600);
                });
            }

            @Override
            public void onError(int code, String message) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (progressDialog != null) {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception ignored) {}
                    }

                    // Restore previous selected if new service failed
                    if (prevSelected != null && currentService == null) {
                        HundredGramAiConfig.setSelectedService(prevSelected);
                    }

                    String errText = LocaleController.formatString("TestFailed", R.string.TestFailed, message + " (" + code + ")");
                    BulletinFactory.of(HundredGramEditServiceActivity.this)
                            .createErrorBulletin(errText)
                            .show();
                });
            }
        });
    }

    @Override
    public void onFragmentDestroy() {
        if (testRequest != null) {
            testRequest.cancel();
            testRequest = null;
        }
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (Exception ignored) {}
        }
        super.onFragmentDestroy();
    }
}
