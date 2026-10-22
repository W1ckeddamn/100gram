package com.exteragram.messenger.nowplaying;

public enum ServiceEmoji {
    MUSIC(5271627010681108586L),
    SPOTIFY(5271857023359681001L),
    TELEGRAM(5325674462522144646L);

    private final long documentId;

    ServiceEmoji(long documentId) {
        this.documentId = documentId;
    }

    public long getDocumentId() {
        return documentId;
    }

    public static ServiceEmoji fromString(String name) {
        if (name == null) {
            return MUSIC;
        }
        for (ServiceEmoji emoji : values()) {
            if (emoji.name().equalsIgnoreCase(name)) {
                return emoji;
            }
        }
        return MUSIC;
    }
}