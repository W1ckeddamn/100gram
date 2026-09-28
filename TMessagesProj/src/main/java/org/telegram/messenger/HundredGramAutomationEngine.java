package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.util.ArrayList;

public class HundredGramAutomationEngine implements NotificationCenter.NotificationCenterDelegate {

    private static volatile HundredGramAutomationEngine[] Instance = new HundredGramAutomationEngine[UserConfig.MAX_ACCOUNT_COUNT];

    public static HundredGramAutomationEngine getInstance(int num) {
        HundredGramAutomationEngine localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (HundredGramAutomationEngine.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new HundredGramAutomationEngine(num);
                }
            }
        }
        return localInstance;
    }

    private final int currentAccount;
    private final ArrayList<HundredGramAutomationRule> rules = new ArrayList<>();
    private boolean started = false;
    private boolean loaded = false;

    private HundredGramAutomationEngine(int account) {
        this.currentAccount = account;
        loadRules();
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didReceiveNewMessages);
        });
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }
        started = false;
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didReceiveNewMessages);
        });
    }

    public synchronized ArrayList<HundredGramAutomationRule> getRules() {
        if (!loaded) {
            loadRules();
        }
        return new ArrayList<>(rules);
    }

    public synchronized void addRule(HundredGramAutomationRule rule) {
        if (rule == null) return;
        rules.add(rule);
        saveRules();
    }

    public synchronized void updateRule(HundredGramAutomationRule rule) {
        if (rule == null) return;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).id == rule.id) {
                rules.set(i, rule);
                saveRules();
                return;
            }
        }
        rules.add(rule);
        saveRules();
    }

    public synchronized void deleteRule(long ruleId) {
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).id == ruleId) {
                rules.remove(i);
                saveRules();
                return;
            }
        }
    }

    public synchronized void toggleRule(long ruleId, boolean enabled) {
        for (HundredGramAutomationRule rule : rules) {
            if (rule.id == ruleId) {
                rule.enabled = enabled;
                saveRules();
                return;
            }
        }
    }

    public synchronized void loadRules() {
        rules.clear();
        loaded = true;
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_automation_rules_" + currentAccount, Context.MODE_PRIVATE);
            String jsonString = prefs.getString("rules", null);
            if (jsonString != null) {
                JSONArray arr = new JSONArray(jsonString);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    HundredGramAutomationRule rule = HundredGramAutomationRule.fromJsonObject(obj);
                    if (rule != null) {
                        rules.add(rule);
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public synchronized void saveRules() {
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_automation_rules_" + currentAccount, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray();
            for (HundredGramAutomationRule rule : rules) {
                arr.put(rule.toJsonObject());
            }
            prefs.edit().putString("rules", arr.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            if (account != currentAccount) {
                return;
            }
            if (args.length > 2 && args[2] instanceof Boolean) {
                boolean scheduled = (Boolean) args[2];
                if (scheduled) {
                    return;
                }
            }
            if (args.length > 1 && args[1] instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<MessageObject> messages = (ArrayList<MessageObject>) args[1];
                processIncomingMessages(messages);
            }
        }
    }

    private void processIncomingMessages(ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        ArrayList<HundredGramAutomationRule> activeRules;
        synchronized (this) {
            if (rules.isEmpty()) {
                return;
            }
            activeRules = new ArrayList<>(rules);
        }

        for (int i = 0; i < messages.size(); i++) {
            MessageObject msg = messages.get(i);
            if (msg == null || msg.isOutOwner()) {
                continue;
            }

            for (HundredGramAutomationRule rule : activeRules) {
                if (rule != null && rule.enabled && rule.matches(msg)) {
                    executeRule(rule, msg);
                }
            }
        }
    }

    private void executeRule(HundredGramAutomationRule rule, MessageObject msg) {
        FileLog.d("HundredGramAutomation: executing rule '" + rule.name + "' for msg " + msg.getId() + " in dialog " + msg.getDialogId());

        // 1. Save to Saved Messages
        if (rule.saveToSaved) {
            long clientUserId = UserConfig.getInstance(currentAccount).getClientUserId();
            ArrayList<MessageObject> forwardList = new ArrayList<>();
            forwardList.add(msg);
            SendMessagesHelper.getInstance(currentAccount).sendMessage(forwardList, clientUserId, false, false, true, 0, 0);
        }

        // 2. Forward to another chat
        if (rule.forwardToChatId != 0) {
            ArrayList<MessageObject> forwardList = new ArrayList<>();
            forwardList.add(msg);
            SendMessagesHelper.getInstance(currentAccount).sendMessage(forwardList, rule.forwardToChatId, false, false, true, 0, 0);
        }

        // 3. Mark as read
        if (rule.markAsRead) {
            int date = msg.messageOwner != null ? msg.messageOwner.date : (int) (System.currentTimeMillis() / 1000);
            MessagesController.getInstance(currentAccount).markDialogAsRead(msg.getDialogId(), msg.getId(), 0, date, false, 0, 0, true, 0);
            MessagesController.getInstance(currentAccount).markDialogAsReadServer(msg.getDialogId(), msg.getId(), date, 0);
        }

        // 4. Save to gallery
        if (rule.saveToGallery && (msg.isPhoto() || msg.isVideo())) {
            File file = FileLoader.getInstance(currentAccount).getPathToMessage(msg.messageOwner);
            if (file != null && file.exists()) {
                MediaController.saveFile(file.getAbsolutePath(), ApplicationLoader.applicationContext, msg.isVideo() ? 1 : 0, null, null);
            }
        }

        // 5. Priority notification
        if (rule.priorityNotify) {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    BulletinFactory factory = BulletinFactory.global();
                    if (factory != null) {
                        String text = LocaleController.formatString("AutomationTriggered", org.telegram.messenger.R.string.AutomationTriggered, rule.name);
                        factory.createSimpleBulletin(org.telegram.messenger.R.raw.chats_infotip, text).show();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }
    }
}
