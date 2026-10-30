package org.telegram.ui;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Shows the answer breakdown of a poll without voting (OpexGram "poll_results_preview").
 * Public polls are read through messages.getPollVotes; anonymous ones through a vote that is
 * retracted right away, after an explicit confirmation.
 */
public class HundredGramPollPeek {

    private static final HashSet<Long> inProgress = new HashSet<>();

    private static TLRPC.TL_messageMediaPoll getMedia(MessageObject msg) {
        if (msg == null || msg.messageOwner == null) {
            return null;
        }
        TLRPC.MessageMedia media = MessageObject.getMedia(msg.messageOwner);
        return media instanceof TLRPC.TL_messageMediaPoll ? (TLRPC.TL_messageMediaPoll) media : null;
    }

    public static boolean canPeek(MessageObject msg) {
        if (!HundredGramConfig.showResultsBeforeVoting || msg == null || msg.scheduled || inProgress.contains(msg.getPollId())) {
            return false;
        }
        final TLRPC.TL_messageMediaPoll media = getMedia(msg);
        if (media == null || media.poll == null || media.results == null) {
            return false;
        }
        final TLRPC.Poll poll = media.poll;
        if (poll.closed || poll.quiz || poll.creator || poll.hide_results_until_close) {
            return false;
        }
        if (poll.answers == null || poll.answers.isEmpty() || media.results.total_voters <= 0) {
            return false;
        }
        if (MessageObject.isVoteResultsIsNotEmpty(media)) {
            return false;
        }
        return poll.public_voters || !poll.revoting_disabled;
    }

    public static void peek(BaseFragment fragment, ChatMessageCell cell, MessageObject msg) {
        if (fragment == null || fragment.getParentActivity() == null || !canPeek(msg)) {
            return;
        }
        final int account = fragment.getCurrentAccount();
        final TLRPC.TL_messageMediaPoll media = getMedia(msg);
        if (media.poll.public_voters) {
            fetchCounts(fragment, cell, msg, media, account);
        } else {
            confirm(fragment, cell, msg, media, account);
        }
    }

    private static void fetchCounts(BaseFragment fragment, ChatMessageCell cell, MessageObject msg, TLRPC.TL_messageMediaPoll media, int account) {
        final ArrayList<TLRPC.PollAnswer> answers = media.poll.answers;
        final TLRPC.PollAnswerVoters[] voters = new TLRPC.PollAnswerVoters[answers.size()];
        final int[] left = {answers.size()};
        final boolean[] failed = {false};
        inProgress.add(msg.getPollId());
        for (int a = 0; a < answers.size(); a++) {
            final int index = a;
            TLRPC.TL_messages_getPollVotes req = new TLRPC.TL_messages_getPollVotes();
            req.peer = MessagesController.getInstance(account).getInputPeer(msg.getDialogId());
            req.id = msg.getId();
            req.option = answers.get(a).option;
            req.limit = 1;
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (response instanceof TLRPC.TL_messages_votesList) {
                    TLRPC.TL_pollAnswerVoters votersCount = new TLRPC.TL_pollAnswerVoters();
                    votersCount.flags = 4;
                    votersCount.option = answers.get(index).option;
                    votersCount.voters = ((TLRPC.TL_messages_votesList) response).count;
                    voters[index] = votersCount;
                } else {
                    failed[0] = true;
                }
                if (--left[0] > 0) {
                    return;
                }
                inProgress.remove(msg.getPollId());
                if (failed[0]) {
                    confirm(fragment, cell, msg, media, account);
                    return;
                }
                ArrayList<TLRPC.PollAnswerVoters> result = new ArrayList<>();
                for (TLRPC.PollAnswerVoters v : voters) {
                    if (v != null) {
                        result.add(v);
                    }
                }
                show(msg, media, result, media.results.total_voters);
            }));
        }
    }

    private static void confirm(BaseFragment fragment, ChatMessageCell cell, MessageObject msg, TLRPC.TL_messageMediaPoll media, int account) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        final int totalVoters = media.results.total_voters;
        if (HundredGramConfig.pollPeekWarned) {
            voteAndRetract(fragment, cell, msg, media, account, totalVoters);
            return;
        }
        new AlertDialog.Builder(fragment.getContext(), fragment.getResourceProvider())
                .setTitle(LocaleController.getString(R.string.PollPeekTitle))
                .setMessage(LocaleController.getString(R.string.PollPeekText))
                .setPositiveButton(LocaleController.getString(R.string.PollPeekButton), (dialog, which) -> {
                    HundredGramConfig.setPollPeekWarned();
                    voteAndRetract(fragment, cell, msg, media, account, totalVoters);
                })
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .show();
    }

    private static void voteAndRetract(BaseFragment fragment, ChatMessageCell cell, MessageObject msg, TLRPC.TL_messageMediaPoll media, int account, int totalVoters) {
        final long pollId = msg.getPollId();
        final byte[] option = media.poll.answers.get(0).option;
        inProgress.add(pollId);
        ArrayList<TLRPC.PollAnswer> answers = new ArrayList<>();
        answers.add(media.poll.answers.get(0));
        SendMessagesHelper.getInstance(account).sendVote(msg, answers, () -> {
            // snapshot while the vote is still counted, the retract response may clear the results
            final ArrayList<TLRPC.PollAnswerVoters> peeked = subtractOwnVote(media, option);
            SendMessagesHelper.getInstance(account).sendVote(msg, null, () -> {
                inProgress.remove(pollId);
                if (peeked == null) {
                    if (fragment.getParentActivity() != null) {
                        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.ErrorOccurred)).show();
                    }
                    return;
                }
                show(msg, media, peeked, totalVoters);
            });
        });
    }

    private static ArrayList<TLRPC.PollAnswerVoters> subtractOwnVote(TLRPC.TL_messageMediaPoll media, byte[] option) {
        if (!MessageObject.isVoteResultsIsNotEmpty(media)) {
            return null;
        }
        ArrayList<TLRPC.PollAnswerVoters> result = new ArrayList<>();
        for (TLRPC.PollAnswerVoters voters : media.results.results) {
            TLRPC.TL_pollAnswerVoters copy = new TLRPC.TL_pollAnswerVoters();
            copy.flags = 4;
            copy.option = voters.option;
            copy.voters = voters.chosen && Arrays.equals(voters.option, option) ? Math.max(0, voters.voters - 1) : voters.voters;
            result.add(copy);
        }
        return result;
    }

    private static void show(MessageObject msg, TLRPC.TL_messageMediaPoll media, ArrayList<TLRPC.PollAnswerVoters> results, int totalVoters) {
        media.results.results = results;
        media.results.flags |= 2;
        media.results.total_voters = totalVoters;
        msg.forceShowPollResults = true;
        NotificationCenter.getInstance(msg.currentAccount).postNotificationName(NotificationCenter.didUpdatePollResults, msg.getPollId(), media.poll, media.results);
    }
}
