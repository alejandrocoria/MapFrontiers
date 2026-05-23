package games.alejandrocoria.mapfrontiers.common.util;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class DebouncedPersistenceController {
    private final long saveDebounceMs;
    private final long saveMaxDelayMs;
    private boolean dirty = false;
    private long lastUpdateAt = 0L;
    private long lastSaveAt = 0L;

    public DebouncedPersistenceController(long saveDebounceMs, long saveMaxDelayMs) {
        this.saveDebounceMs = saveDebounceMs;
        this.saveMaxDelayMs = saveMaxDelayMs;
    }

    public boolean hasPendingChanges() {
        return dirty;
    }

    public void markDirty(long now) {
        dirty = true;
        lastUpdateAt = now;
    }

    public boolean shouldFlushOnTick(long now) {
        if (!dirty) {
            return false;
        }

        boolean debounceElapsed = now - lastUpdateAt >= saveDebounceMs;
        boolean maxDelayElapsed = lastSaveAt == 0L || now - lastSaveAt >= saveMaxDelayMs;
        return debounceElapsed || maxDelayElapsed;
    }

    public void markPersisted(long now) {
        dirty = false;
        lastSaveAt = now;
    }

    public void reset() {
        dirty = false;
        lastUpdateAt = 0L;
        lastSaveAt = 0L;
    }
}
