package org.telegram.messenger;

import android.text.TextUtils;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Контроллер очереди отправки сообщений при активном Slowmode (медленном режиме).
 * Если в группе включён Slowmode, сообщения не отклоняются с ошибкой,
 * а помещаются в локальную очередь и автоматически отправляются,
 * как только истечёт время задержки.
 */
public class HundredGramSlowmodeController {

    private static final HundredGramSlowmodeController[] instances = new HundredGramSlowmodeController[UserConfig.MAX_ACCOUNT_COUNT];

    public static HundredGramSlowmodeController getInstance(int currentAccount) {
        HundredGramSlowmodeController local = instances[currentAccount];
        if (local == null) {
            synchronized (HundredGramSlowmodeController.class) {
                local = instances[currentAccount];
                if (local == null) {
                    local = instances[currentAccount] = new HundredGramSlowmodeController(currentAccount);
                }
            }
        }
        return local;
    }

    public static class QueuedMessage {
        public long dialogId;
        public Runnable sendAction;
        public CharSequence messageText;
        public long queueTime;

        public QueuedMessage(long dialogId, Runnable sendAction, CharSequence messageText) {
            this.dialogId = dialogId;
            this.sendAction = sendAction;
            this.messageText = messageText;
            this.queueTime = System.currentTimeMillis();
        }
    }

    private final int currentAccount;
    private final HashMap<Long, LinkedList<QueuedMessage>> queues = new HashMap<>();
    private final HashMap<Long, Runnable> scheduledRunnables = new HashMap<>();

    public HundredGramSlowmodeController(int account) {
        this.currentAccount = account;
    }

    public boolean isQueueActive(long dialogId) {
        LinkedList<QueuedMessage> q = queues.get(dialogId);
        return q != null && !q.isEmpty();
    }

    public int getQueueCount(long dialogId) {
        LinkedList<QueuedMessage> q = queues.get(dialogId);
        return q != null ? q.size() : 0;
    }

    public long getRemainingSeconds(long dialogId) {
        TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(-dialogId);
        if (chatFull == null || chatFull.slowmode_seconds <= 0) {
            return 0;
        }
        int currentTime = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        return Math.max(0, chatFull.slowmode_next_send_date - currentTime);
    }

    public void enqueue(BaseFragment fragment, long dialogId, CharSequence messageText, Runnable sendAction) {
        if (!HundredGramConfig.slowmodeQueue || sendAction == null) {
            if (sendAction != null) {
                sendAction.run();
            }
            return;
        }

        long seconds = getRemainingSeconds(dialogId);
        if (seconds <= 0 && !isQueueActive(dialogId)) {
            sendAction.run();
            return;
        }

        synchronized (queues) {
            LinkedList<QueuedMessage> q = queues.computeIfAbsent(dialogId, k -> new LinkedList<>());
            q.add(new QueuedMessage(dialogId, sendAction, messageText));
        }

        if (fragment instanceof ChatActivity) {
            try {
                String waitStr = LocaleController.formatPluralString("Seconds", (int) Math.max(1, seconds));
                String text = LocaleController.formatString("SlowmodeQueued", R.string.SlowmodeQueued, waitStr);
                BulletinFactory.of(fragment).createSimpleBulletin(R.drawable.input_schedule, text).show();
            } catch (Exception ignored) {}
        }

        scheduleNext(dialogId);
    }

    private void scheduleNext(long dialogId) {
        synchronized (queues) {
            LinkedList<QueuedMessage> q = queues.get(dialogId);
            if (q == null || q.isEmpty()) {
                queues.remove(dialogId);
                scheduledRunnables.remove(dialogId);
                return;
            }

            if (scheduledRunnables.containsKey(dialogId)) {
                return;
            }

            long seconds = getRemainingSeconds(dialogId);
            long delayMs = Math.max(500, (seconds * 1000L) + 500);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    QueuedMessage nextMsg = null;
                    synchronized (queues) {
                        scheduledRunnables.remove(dialogId);
                        LinkedList<QueuedMessage> qInner = queues.get(dialogId);
                        if (qInner != null && !qInner.isEmpty()) {
                            nextMsg = qInner.poll();
                        }
                    }
                    if (nextMsg != null && nextMsg.sendAction != null) {
                        try {
                            nextMsg.sendAction.run();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                    scheduleNext(dialogId);
                }
            };

            scheduledRunnables.put(dialogId, r);
            AndroidUtilities.runOnUIThread(r, delayMs);
        }
    }

    public void handleSlowmodeWait(long dialogId, int waitSeconds) {
        if (!HundredGramConfig.slowmodeQueue || waitSeconds <= 0) {
            return;
        }
        TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(-dialogId);
        if (chatFull != null) {
            chatFull.slowmode_next_send_date = ConnectionsManager.getInstance(currentAccount).getCurrentTime() + waitSeconds;
        }

        synchronized (queues) {
            Runnable r = scheduledRunnables.remove(dialogId);
            if (r != null) {
                AndroidUtilities.cancelRunOnUIThread(r);
            }
        }
        scheduleNext(dialogId);
    }

    public void clearQueue(long dialogId) {
        synchronized (queues) {
            Runnable r = scheduledRunnables.remove(dialogId);
            if (r != null) {
                AndroidUtilities.cancelRunOnUIThread(r);
            }
            queues.remove(dialogId);
        }
    }
}
