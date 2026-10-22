package org.telegram.messenger.ai;

import java.io.Serializable;
import java.util.Objects;

public final class Message implements Serializable {

    private final String role;
    private final String content;
    private byte[] imageData;
    private String mimeType;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public Message(String role, String content, byte[] imageData, String mimeType) {
        this.role = role;
        this.content = content;
        this.imageData = imageData;
        this.mimeType = mimeType;
    }

    public String role() {
        return role;
    }

    public String content() {
        return content;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || Message.class != obj.getClass()) return false;
        Message other = (Message) obj;
        return Objects.equals(this.role, other.role) && Objects.equals(this.content, other.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }
}
