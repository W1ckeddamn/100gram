package org.telegram.ui.Components;

import org.telegram.ui.ActionBar.Theme;

public abstract class PreviewColors {
    public static int getBackgroundColor() {
        return Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), Theme.isCurrentThemeDark() ? 0.05f : 0.035f);
    }

    public static int getOutlineColor() {
        return Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), (Theme.isCurrentThemeDark() ? 0.05f : 0.035f) + 0.085f);
    }

    public static int getMockColor(boolean bright) {
        return Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2), bright ? 0.4f : 0.2f);
    }
}
