package org.telegram.messenger;

import java.util.ArrayList;
import java.util.List;

public final class HundredGramAccountNotifications {

    private HundredGramAccountNotifications() {
    }

    private static String key(int account) {
        return "accountNotifications_" + UserConfig.getInstance(account).clientUserId;
    }

    public static boolean isEnabled(int account) {
        return MessagesController.getGlobalNotificationsSettings().getBoolean(key(account), true);
    }

    public static void setEnabled(int account, boolean enabled) {
        MessagesController.getGlobalNotificationsSettings().edit().putBoolean(key(account), enabled).apply();
        apply(account);
    }

    public static boolean shouldShow(int account) {
        if (account != UserConfig.selectedAccount) {
            return SharedConfig.showNotificationsForAllAccounts && isEnabled(account);
        }
        return true;
    }

    public static void apply(int account) {
        NotificationsController notificationsController = NotificationsController.getInstance(account);
        if (shouldShow(account)) {
            notificationsController.showNotifications();
        } else {
            notificationsController.hideNotifications();
        }
    }

    public static void applyAll() {
        for (int account : getAccounts()) {
            apply(account);
        }
    }

    public static List<Integer> getAccounts() {
        ArrayList<Integer> accounts = new ArrayList<>();
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (UserConfig.getInstance(i).isClientActivated()) {
                accounts.add(i);
            }
        }
        return accounts;
    }

    public static int getEnabledCount() {
        int count = 0;
        for (int account : getAccounts()) {
            if (account == UserConfig.selectedAccount || isEnabled(account)) {
                count++;
            }
        }
        return count;
    }
}
