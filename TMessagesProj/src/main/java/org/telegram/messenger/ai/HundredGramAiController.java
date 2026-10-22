package org.telegram.messenger.ai;

import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HundredGramAiController {

    private static volatile HundredGramAiController instance;

    private final List<Role> customRoles = new ArrayList<>();
    private final List<Role> suggestedRoles = new ArrayList<>();

    public static HundredGramAiController getInstance() {
        if (instance == null) {
            synchronized (HundredGramAiController.class) {
                if (instance == null) {
                    instance = new HundredGramAiController();
                }
            }
        }
        return instance;
    }

    private HundredGramAiController() {
        for (Suggestions s : Suggestions.values()) {
            suggestedRoles.add(s.getRole());
        }
        loadRoles();
    }

    public static boolean canUseAi() {
        if (!HundredGramAiConfig.isAiEnabled()) {
            return false;
        }
        Service s = HundredGramAiConfig.getSelectedService();
        return s != null && !TextUtils.isEmpty(s.getKey());
    }

    public List<Role> getSuggestedRoles() {
        return Collections.unmodifiableList(suggestedRoles);
    }

    public List<Role> getCustomRoles() {
        return Collections.unmodifiableList(customRoles);
    }

    public List<Role> getAllRoles() {
        ArrayList<Role> all = new ArrayList<>(suggestedRoles);
        all.addAll(customRoles);
        return all;
    }

    public Role getSelectedRole() {
        String selectedName = HundredGramAiConfig.getSelectedRole();
        for (Role r : getAllRoles()) {
            if (r.getName().equals(selectedName)) {
                return r;
            }
        }
        return Suggestions.ASSISTANT.getRole();
    }

    public void setSelectedRole(Role role) {
        if (role != null) {
            HundredGramAiConfig.setSelectedRole(role.getName());
        }
    }

    public void loadRoles() {
        customRoles.clear();
        customRoles.addAll(HundredGramAiConfig.getRoles());
    }

    public void addRole(Role role) {
        if (role == null || isSuggested(role)) return;
        customRoles.add(0, role);
        HundredGramAiConfig.saveRoles(customRoles);
    }

    public boolean removeRole(Role role) {
        if (role == null || isSuggested(role)) return false;
        boolean removed = customRoles.remove(role);
        if (removed) {
            HundredGramAiConfig.saveRoles(customRoles);
        }
        return removed;
    }

    public boolean updateRole(Role oldRole, Role newRole) {
        if (oldRole == null || newRole == null || isSuggested(oldRole)) return false;
        int idx = customRoles.indexOf(oldRole);
        if (idx >= 0) {
            customRoles.set(idx, newRole);
            HundredGramAiConfig.saveRoles(customRoles);
            return true;
        } else {
            addRole(newRole);
            return true;
        }
    }

    public boolean isSuggested(Role role) {
        if (role == null) return false;
        for (Role s : suggestedRoles) {
            if (s.getName().equals(role.getName())) return true;
        }
        return false;
    }

    public HundredGramAiClient.Cancellable summarize(String text, Utilities.Callback<String> onSuccess, Utilities.Callback2<Integer, String> onError) {
        return HundredGramAiClient.request(text, Suggestions.SUMMARIZER.getRole(), false, new HundredGramAiClient.GenerationCallback() {
            @Override
            public void onChunk(String accumulatedText, String newDelta) {}

            @Override
            public void onResponse(String fullText) {
                if (onSuccess != null) {
                    onSuccess.run(fullText);
                }
            }

            @Override
            public void onError(int code, String message) {
                if (onError != null) {
                    onError.run(code, message);
                }
            }
        });
    }

    public HundredGramAiClient.Cancellable proofread(String text, Utilities.Callback<String> onSuccess, Utilities.Callback2<Integer, String> onError) {
        return HundredGramAiClient.request(text, Suggestions.PROOFREADER.getRole(), false, new HundredGramAiClient.GenerationCallback() {
            @Override
            public void onChunk(String accumulatedText, String newDelta) {}

            @Override
            public void onResponse(String fullText) {
                if (onSuccess != null) {
                    onSuccess.run(fullText);
                }
            }

            @Override
            public void onError(int code, String message) {
                if (onError != null) {
                    onError.run(code, message);
                }
            }
        });
    }

    public static void clearHistory(BaseFragment fragment, Runnable onCleared) {
        if (fragment == null || fragment.getParentActivity() == null) {
            HundredGramAiConfig.clearConversationHistory();
            if (onCleared != null) onCleared.run();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(LocaleController.getString("ClearHistory", R.string.ClearHistory));
        builder.setMessage(LocaleController.getString("ClearConversationHistoryInfo", R.string.ClearConversationHistoryInfo));
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton), (dialog, which) -> {
            HundredGramAiConfig.clearConversationHistory();
            if (onCleared != null) onCleared.run();
            if (BulletinFactory.canShowBulletin(fragment)) {
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.ic_delete, LocaleController.getString("HundredGramAiHistoryCleared", R.string.HundredGramAiHistoryCleared)).show();
            }
        });
        fragment.showDialog(builder.create());
    }
}
