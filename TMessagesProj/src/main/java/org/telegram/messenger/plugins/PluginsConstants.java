package org.telegram.messenger.plugins;

public final class PluginsConstants {
    public static final String PLUGINS = "plugins";
    public static final String PLUGINS_EXT = ".plugin";
    public static final String NATIVE_EXT = ".100p";
    public static final String DEX_EXT = ".dex";
    public static final String ELYX_EXT = ".elyx";
    public static final String EAF_EXT = ".eaf";
    public static final String ZIP_EXT = ".zip";

    public static final String ENGINE_NATIVE = "native";
    public static final String ENGINE_PYTHON = "python";

    public static final String ON_PLUGIN_LOAD = "on_plugin_load";
    public static final String ON_PLUGIN_UNLOAD = "on_plugin_unload";
    public static final String ON_APP_EVENT = "on_app_event";
    public static final String CREATE_SETTINGS = "create_settings";

    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String UPDATE = "update";
    public static final String UPDATES = "updates";
    public static final String PARAMS = "params";

    private PluginsConstants() {}

    public static final class MenuItemTypes {
        public static final String MESSAGE_CONTEXT_MENU = "message_context_menu";
        public static final String CHAT_ACTION_MENU = "chat_action_menu";
        public static final String PROFILE_ACTION_MENU = "profile_action_menu";
        public static final String MAIN_MENU = "main_menu";
        public static final String DRAWER_MENU = "drawer_menu";

        private MenuItemTypes() {}
    }

    public static final class MenuItemProperties {
        public static final String ITEM_ID = "item_id";
        public static final String MENU_TYPE = "menu_type";
        public static final String TEXT = "text";
        public static final String SUBTEXT = "subtext";
        public static final String ICON = "icon";
        public static final String CONDITION = "condition";
        public static final String PRIORITY = "priority";
        public static final String ON_CLICK = "on_click";

        private MenuItemProperties() {}
    }

    public static final class Strategy {
        public static final String CANCEL = "CANCEL";
        public static final String DEFAULT = "DEFAULT";
        public static final String MODIFY = "MODIFY";

        private Strategy() {}
    }

    public static final class Settings {
        public static final String KEY = "key";
        public static final String TYPE = "type";
        public static final String TEXT = "text";
        public static final String SUBTEXT = "subtext";
        public static final String ICON = "icon";
        public static final String DEFAULT = "default";
        public static final String HINT = "hint";
        public static final String ON_CHANGE = "on_change";
        public static final String ON_CLICK = "on_click";
        public static final String LINK_ALIAS = "link_alias";

        public static final String TYPE_HEADER = "header";
        public static final String TYPE_SWITCH = "switch";
        public static final String TYPE_INPUT = "input";
        public static final String TYPE_SELECTOR = "selector";
        public static final String TYPE_TEXT = "text";
        public static final String TYPE_DIVIDER = "divider";
        public static final String TYPE_EDIT_TEXT = "edit_text";

        private Settings() {}
    }
}
