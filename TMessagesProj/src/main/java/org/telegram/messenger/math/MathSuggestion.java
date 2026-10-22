package org.telegram.messenger.math;

public final class MathSuggestion {

    private final int insertAt;
    private final String insertText;
    private final String value;

    public MathSuggestion(int insertAt, String insertText, String value) {
        this.insertAt = insertAt;
        this.insertText = insertText;
        this.value = value;
    }

    public int getInsertAt() {
        return insertAt;
    }

    public String getInsertText() {
        return insertText;
    }

    public String getValue() {
        return value;
    }
}
