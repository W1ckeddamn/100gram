package org.telegram.messenger.ai;

import java.io.Serializable;
import java.util.Objects;

public class Role implements Comparable<Role>, Serializable {

    private String name;
    private String prompt;
    private long emojiId;
    private boolean isSuggestion;

    public Role(String name, String prompt) {
        this.name = name;
        this.prompt = prompt;
    }

    @Override
    public int compareTo(Role role) {
        if (role == null || role.name == null) return 1;
        if (this.name == null) return -1;
        return this.name.compareTo(role.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Role other = (Role) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public long getEmojiId() {
        return emojiId;
    }

    public Role setEmojiId(long emojiId) {
        this.emojiId = emojiId;
        return this;
    }

    public boolean isSuggestion() {
        return isSuggestion;
    }

    public Role setSuggestion(boolean suggestion) {
        this.isSuggestion = suggestion;
        return this;
    }

    public boolean isSelected() {
        return Objects.equals(HundredGramAiConfig.getSelectedRole(), this.name);
    }
}
