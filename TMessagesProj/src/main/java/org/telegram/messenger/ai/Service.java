package org.telegram.messenger.ai;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Service implements Serializable {

    private String id;
    private String name;
    private String url;
    private String model;
    private String key;
    private boolean reasoningEnabled;

    public Service(String name, String url, String model, String key) {
        this(UUID.randomUUID().toString(), name, url, model, key, false);
    }

    public Service(String id, String name, String url, String model, String key) {
        this(id, name, url, model, key, false);
    }

    public Service(String id, String name, String url, String model, String key, boolean reasoningEnabled) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.model = model;
        this.key = key;
        this.reasoningEnabled = reasoningEnabled;
    }

    public String getId() {
        ensureId();
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean ensureId() {
        if (id != null && !id.isEmpty()) {
            return false;
        }
        id = UUID.randomUUID().toString();
        return true;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? getShortModel() : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getShortModel() {
        if (model == null) return "";
        String[] parts = model.split("/");
        String lastPart = parts[parts.length - 1];
        int colon = lastPart.indexOf(':');
        return colon != -1 ? lastPart.substring(0, colon) : lastPart;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isReasoningEnabled() {
        return reasoningEnabled;
    }

    public void setReasoningEnabled(boolean reasoningEnabled) {
        this.reasoningEnabled = reasoningEnabled;
    }

    public boolean isSelected() {
        return Objects.equals(HundredGramAiConfig.getSelectedServiceId(), getId());
    }

    public boolean isGemini() {
        String lower = (url != null ? url : "").toLowerCase();
        return lower.contains("generativelanguage.googleapis.com") || lower.contains("google") || "gemini".equalsIgnoreCase(name);
    }

    public boolean isOpenAi() {
        String lower = (url != null ? url : "").toLowerCase();
        return lower.contains("api.openai.com") || "openai".equalsIgnoreCase(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Service other = (Service) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
