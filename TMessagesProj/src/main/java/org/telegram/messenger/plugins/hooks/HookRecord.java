package org.telegram.messenger.plugins.hooks;

public interface HookRecord {
    void cleanup();
    boolean matches(Object criteria);
}
