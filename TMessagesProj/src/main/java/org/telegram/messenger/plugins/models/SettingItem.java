package org.telegram.messenger.plugins.models;

public abstract class SettingItem {
    private String type;
    private String icon;
    private String linkAlias;

    public SettingItem(String type, String icon, String linkAlias) {
        this.type = type;
        this.icon = icon;
        this.linkAlias = linkAlias;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLinkAlias() {
        return linkAlias;
    }

    public void setLinkAlias(String linkAlias) {
        this.linkAlias = linkAlias;
    }

    public void cleanup() {
    }
}
