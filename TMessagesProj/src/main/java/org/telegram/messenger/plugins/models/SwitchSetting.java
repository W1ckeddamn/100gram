package org.telegram.messenger.plugins.models;

import org.telegram.messenger.plugins.PluginsConstants;

public class SwitchSetting extends SettingItem {
    private String key;
    private String text;
    private String subtext;
    private boolean defaultValue;
    private Runnable onChange;

    public SwitchSetting(String key, String text, String subtext, boolean defaultValue, String icon) {
        super(PluginsConstants.Settings.TYPE_SWITCH, icon, null);
        this.key = key;
        this.text = text;
        this.subtext = subtext;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSubtext() {
        return subtext;
    }

    public void setSubtext(String subtext) {
        this.subtext = subtext;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public Runnable getOnChange() {
        return onChange;
    }
}
