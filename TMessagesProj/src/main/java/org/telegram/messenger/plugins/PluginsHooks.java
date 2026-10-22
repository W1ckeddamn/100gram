package org.telegram.messenger.plugins;

import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public interface PluginsHooks {

    PostRequestResult executePostRequestHook(String requestName, int account, TLObject response, TLRPC.TL_error error);

    TLObject executePreRequestHook(String requestName, int account, TLObject request);

    SendMessagesHelper.SendMessageParams executeSendMessageHook(int account, SendMessagesHelper.SendMessageParams params);

    TLRPC.Update executeUpdateHook(String updateName, int account, TLRPC.Update update);

    TLRPC.Updates executeUpdatesHook(String containerName, int account, TLRPC.Updates updates);

    final class PostRequestResult {
        private TLRPC.TL_error error;
        private TLObject response;

        public PostRequestResult(TLObject tLObject, TLRPC.TL_error tL_error) {
            this.response = tLObject;
            this.error = tL_error;
        }

        public TLObject getResponse() {
            return this.response;
        }

        public void setResponse(TLObject tLObject) {
            this.response = tLObject;
        }

        public TLRPC.TL_error getError() {
            return this.error;
        }

        public void setError(TLRPC.TL_error tL_error) {
            this.error = tL_error;
        }
    }
}
