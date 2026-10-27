package org.telegram.ui;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Deletes every message you sent in the given dialogs, 100 at a time,
 * through messages.search with from_id = self.
 */
public class HundredGramDeleteMyMessages {

    private static boolean running;
    private static int deleted;
    private static int current;
    private static ArrayList<Long> dialogIds;
    private static AlertDialog progressDialog;

    public static void show(BaseFragment fragment, long dialogId) {
        show(fragment, new ArrayList<>(Collections.singletonList(dialogId)));
    }

    public static void show(BaseFragment fragment, ArrayList<Long> ids) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        ArrayList<Long> valid = new ArrayList<>();
        for (Long id : ids) {
            if (id != null && id != 0 && !DialogObject.isEncryptedDialog(id)) {
                valid.add(id);
            }
        }
        if (valid.isEmpty()) {
            return;
        }
        if (running) {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.info, LocaleController.getString("DeleteMyMessagesBusy", R.string.DeleteMyMessagesBusy)).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString("DeleteMyMessages", R.string.DeleteMyMessages));
        builder.setMessage(valid.size() == 1
                ? LocaleController.getString("DeleteMyMessagesInfo", R.string.DeleteMyMessagesInfo)
                : String.format(LocaleController.getString("DeleteMyMessagesInfoChats", R.string.DeleteMyMessagesInfoChats), valid.size()));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (d, w) -> start(fragment, valid));
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        fragment.showDialog(builder.create());
    }

    private static void start(BaseFragment fragment, ArrayList<Long> ids) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        running = true;
        deleted = 0;
        current = 0;
        dialogIds = ids;
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString("DeleteMyMessages", R.string.DeleteMyMessages));
        builder.setMessage(progressText());
        builder.setNegativeButton(LocaleController.getString("Stop", R.string.Stop), (d, w) -> finish(fragment, false));
        progressDialog = builder.create();
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnCancelListener(d -> finish(fragment, false));
        fragment.showDialog(progressDialog);
        request(fragment.getCurrentAccount(), dialogIds.get(0), 0, fragment);
    }

    private static void request(int account, long dialogId, int offsetId, BaseFragment fragment) {
        if (!running) {
            return;
        }
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = MessagesController.getInstance(account).getInputPeer(dialogId);
        if (req.peer == null) {
            next(fragment, account);
            return;
        }
        req.flags |= 1;
        req.from_id = new TLRPC.TL_inputPeerSelf();
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        req.q = "";
        req.offset_id = offsetId;
        req.limit = 100;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (!running) {
                return;
            }
            if (!(response instanceof TLRPC.messages_Messages)) {
                finish(fragment, true);
                return;
            }
            ArrayList<Integer> ids = new ArrayList<>();
            int minId = Integer.MAX_VALUE;
            ArrayList<TLRPC.Message> messages = ((TLRPC.messages_Messages) response).messages;
            if (messages != null) {
                for (TLRPC.Message message : messages) {
                    if (message != null && message.id > 0) {
                        ids.add(message.id);
                        minId = Math.min(minId, message.id);
                    }
                }
            }
            if (ids.isEmpty() || offsetId != 0 && minId >= offsetId) {
                next(fragment, account);
                return;
            }
            deleted += ids.size();
            MessagesController.getInstance(account).deleteMessages(ids, null, null, dialogId, 0, true, 0);
            if (progressDialog != null) {
                progressDialog.setMessage(progressText());
            }
            request(account, dialogId, minId, fragment);
        }));
    }

    private static void next(BaseFragment fragment, int account) {
        current++;
        if (current >= dialogIds.size()) {
            finish(fragment, false);
            return;
        }
        if (progressDialog != null) {
            progressDialog.setMessage(progressText());
        }
        request(account, dialogIds.get(current), 0, fragment);
    }

    private static CharSequence progressText() {
        if (dialogIds.size() == 1) {
            return String.format(LocaleController.getString("DeleteMyMessagesProgress", R.string.DeleteMyMessagesProgress), deleted);
        }
        return String.format(LocaleController.getString("DeleteMyMessagesProgressChats", R.string.DeleteMyMessagesProgressChats),
                Math.min(current + 1, dialogIds.size()), dialogIds.size(), deleted);
    }

    private static void finish(BaseFragment fragment, boolean failed) {
        running = false;
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (Throwable ignore) {
            }
            progressDialog = null;
        }
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        CharSequence text;
        if (deleted > 0) {
            text = String.format(LocaleController.getString("DeleteMyMessagesDone", R.string.DeleteMyMessagesDone), deleted);
        } else {
            text = LocaleController.getString("DeleteMyMessagesEmpty", R.string.DeleteMyMessagesEmpty);
        }
        BulletinFactory factory = BulletinFactory.of(fragment);
        if (failed) {
            factory.createSimpleBulletin(R.raw.ic_delete, text, LocaleController.getString("DeleteMyMessagesFailed", R.string.DeleteMyMessagesFailed)).show();
        } else {
            factory.createSimpleBulletin(deleted > 0 ? R.raw.ic_delete : R.raw.info, text).show();
        }
    }
}
