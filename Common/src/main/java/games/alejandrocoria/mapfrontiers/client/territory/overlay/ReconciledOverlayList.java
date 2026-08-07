package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Reuses a dense prefix of slots and retires only the unvisited tail after each reconciliation.
 */
public final class ReconciledOverlayList<S extends OverlaySlot<?>> {
    private final List<S> slots = new ArrayList<>();
    private final Supplier<? extends S> slotFactory;
    private boolean reconciling;
    private int cursor;

    public ReconciledOverlayList(Supplier<? extends S> slotFactory) {
        this.slotFactory = Objects.requireNonNull(slotFactory, "slotFactory");
    }

    public void beginReconcile() {
        if (reconciling) {
            throw new IllegalStateException("Overlay list is already being reconciled");
        }
        reconciling = true;
        cursor = 0;
    }

    public S acquire() {
        requireReconcile("Slots can only be acquired during reconciliation");
        if (cursor == slots.size()) {
            slots.add(Objects.requireNonNull(slotFactory.get(), "slotFactory result"));
        }
        return slots.get(cursor++);
    }

    public void finishReconcile(OverlayRefreshResult result, String layer) {
        requireReconcile("Overlay list is not being reconciled");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(layer, "layer");

        for (int index = cursor; index < slots.size(); index++) {
            slots.get(index).retire(result, layer);
        }
        if (cursor < slots.size()) {
            slots.subList(cursor, slots.size()).clear();
        }
        reconciling = false;
        cursor = 0;
    }

    public void clear(OverlayRefreshResult result, String layer) {
        if (reconciling) {
            throw new IllegalStateException("Overlay list cannot be cleared during reconciliation");
        }
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(layer, "layer");

        for (S slot : slots) {
            slot.retire(result, layer);
        }
        slots.clear();
    }

    public int size() {
        return slots.size();
    }

    public S get(int index) {
        return slots.get(index);
    }

    private void requireReconcile(String message) {
        if (!reconciling) {
            throw new IllegalStateException(message);
        }
    }
}
