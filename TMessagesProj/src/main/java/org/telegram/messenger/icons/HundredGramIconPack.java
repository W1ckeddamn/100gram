package org.telegram.messenger.icons;

import android.util.SparseIntArray;
import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Модель набора иконок форка 100gram.
 */
public class HundredGramIconPack {
    private final String id;
    private final String name;
    private final String author;
    private final String version;
    private final Map<String, String> icons;
    private final SparseIntArray preinstalledMap;
    private final File location;

    public HundredGramIconPack(String id, String name, String author, String version, Map<String, String> icons, SparseIntArray preinstalledMap, File location) {
        this.id = id != null ? id : "default";
        this.name = name != null ? name : "Default";
        this.author = author != null ? author : "100gram";
        this.version = version != null ? version : "1.0";
        this.icons = icons != null ? icons : Collections.emptyMap();
        this.preinstalledMap = preinstalledMap;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getIcons() {
        return icons;
    }

    public SparseIntArray getPreinstalledMap() {
        return preinstalledMap;
    }

    public File getLocation() {
        return location;
    }

    public boolean isDefault() {
        return "default".equals(id);
    }

    public int getIconCount() {
        if (preinstalledMap != null) {
            return preinstalledMap.size();
        }
        return icons.size();
    }
}
