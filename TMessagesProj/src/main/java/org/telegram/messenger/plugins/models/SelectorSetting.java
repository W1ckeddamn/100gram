package org.telegram.messenger.plugins.models;

import java.util.List;
import org.telegram.messenger.plugins.PluginsConstants;

public class SelectorSetting extends SettingItem {
    private String key;
    private String text;
    private String subtext;
    private int defaultValue;
    private List<String> options;

    public SelectorSetting(String key, String text, String subtext, int defaultValue, List<String> options, String icon) {
        super(PluginsConstants.Settings.TYPE_SELECTOR, icon, null);
        this.key = key;
        this.text = text;
        this.subtext = subtext;
        this.defaultValue = defaultValue;
        this.options = options;
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

    public int getDefaultValue() {
        return defaultValue;
    }

    public List<String> getOptions() {
        return options;
    }
}
