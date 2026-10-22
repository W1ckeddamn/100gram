package org.telegram.messenger.plugins.models;

import org.telegram.messenger.plugins.PluginsConstants;

public class TextSetting extends SettingItem {
    private String text;
    private String subtext;

    public TextSetting(String text, String subtext, String icon) {
        super(PluginsConstants.Settings.TYPE_TEXT, icon, null);
        this.text = text;
        this.subtext = subtext;
    }

    public String getText() {
        return text;
    }

    public String getSubtext() {
        return subtext;
    }
}
