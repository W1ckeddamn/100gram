package org.telegram.messenger.plugins;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.messenger.plugins.models.SettingItem;
import java.io.File;
import java.util.List;
import java.util.Map;

public interface PluginsEngine {

    boolean isPlugin(File file, MessageObject messageObject);

    boolean isEngineAvailable();

    void init(Runnable callback);

    void shutdown(Runnable callback);

    void loadPluginFromFile(String filePath, Plugin metadata, Utilities.Callback<String> callback);

    void setPluginEnabled(String pluginId, boolean enabled, Utilities.Callback<String> callback);

    void deletePlugin(String pluginId, Utilities.Callback<String> callback);

    String getPluginPath(String pluginId);

    List<SettingItem> loadPluginSettings(String pluginId);

    Object getPluginSetting(String pluginId, String key, Object defaultValue);

    void setPluginSetting(String pluginId, String key, Object value);

    void clearPluginSettings(String pluginId);

    Map<String, ?> getAllPluginSettings(String pluginId);

    void openPluginSettings(String pluginId, BaseFragment fragment);

    void openPluginSetting(String pluginId, String linkAlias, BaseFragment fragment);

    void executeOnAppEvent(String eventType);

    PluginsController.HookResult<TLObject> executePreRequestHook(String requestName, int account, TLObject request, String pluginId);

    PluginsController.HookResult<PluginsHooks.PostRequestResult> executePostRequestHook(String requestName, int account, TLObject response, TLRPC.TL_error error, String pluginId);

    PluginsController.HookResult<SendMessagesHelper.SendMessageParams> executeSendMessageHook(int account, SendMessagesHelper.SendMessageParams params, String pluginId);

    PluginsController.HookResult<TLRPC.Update> executeUpdateHook(String updateName, int account, TLRPC.Update update, String pluginId);

    PluginsController.HookResult<TLRPC.Updates> executeUpdatesHook(String containerName, int account, TLRPC.Updates updates, String pluginId);
}
