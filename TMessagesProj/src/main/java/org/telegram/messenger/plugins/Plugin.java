package org.telegram.messenger.plugins;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import java.util.ArrayList;
import java.util.List;

public class Plugin {
    private final String id;
    private final String name;
    private String engine;
    private String version = "1.0";
    private String description;
    private String author;
    private String icon;
    private boolean isEnabled;
    private Throwable error;
    private List<String> requirements = new ArrayList<>();
    private PluginsEngine cachedEngine;

    public Plugin(String id, String name) {
        this.id = id;
        this.name = name;
        try {
            this.description = LocaleController.getString(R.string.PluginNoDescription);
            this.author = LocaleController.getString(R.string.PluginNoAuthor);
        } catch (Throwable e) {
            this.description = "No description";
            this.author = "Unknown";
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isEnabled() {
        return !hasError() && isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled && !hasError();
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
        if (hasError()) {
            this.isEnabled = false;
        }
    }

    public boolean hasError() {
        return error != null;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<String> requirements) {
        this.requirements = requirements;
    }

    public PluginsEngine getCachedEngine() {
        return cachedEngine;
    }

    public void setCachedEngine(PluginsEngine cachedEngine) {
        this.cachedEngine = cachedEngine;
    }
}
