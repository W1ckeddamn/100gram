package org.telegram.messenger.plugins.models;

import org.telegram.messenger.plugins.PluginsConstants;

public class InputSetting extends SettingItem {
    private String key;
    private String text;
    private String subtext;
    private String defaultValue;
    private String hint;

    public InputSetting(String key, String text, String subtext, String defaultValue, String hint, String icon) {
        super(PluginsConstants.Settings.TYPE_INPUT, icon, null);
        this.key = key;
        this.text = text;
        this.subtext = subtext;
        this.defaultValue = defaultValue;
        this.hint = hint;
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public String getSubtext() {
        return subtext;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getHint() {
        return hint;
    }
}
