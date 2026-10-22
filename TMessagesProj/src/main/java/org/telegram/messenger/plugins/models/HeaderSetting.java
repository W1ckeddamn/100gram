package org.telegram.messenger.plugins.models;

import org.telegram.messenger.plugins.PluginsConstants;

public class HeaderSetting extends SettingItem {
    private String text;

    public HeaderSetting(String text) {
        super(PluginsConstants.Settings.TYPE_HEADER, null, null);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
