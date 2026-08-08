package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.MarkerOverlay;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * Reconciles logical marker positions while allowing an absent position to retain a hidden slot.
 */
public final class MarkerOverlayLayer {
    private final String layerName;
    private final ReconciledOverlayList<MarkerOverlaySlot> slots;
    private final BitSet presentSlots = new BitSet();
    private int reconcileIndex;

    public MarkerOverlayLayer(String modId, String layerName, OverlayPublisher publisher) {
        Objects.requireNonNull(modId, "modId");
        this.layerName = Objects.requireNonNull(layerName, "layerName");
        Objects.requireNonNull(publisher, "publisher");
        slots = new ReconciledOverlayList<>(() -> new MarkerOverlaySlot(modId, publisher));
    }

    public void beginReconcile() {
        slots.beginReconcile();
        reconcileIndex = 0;
    }

    public void reconcileNext(@Nullable MarkerOverlayState state, boolean visible, OverlayRefreshResult result) {
        int index = reconcileIndex;
        MarkerOverlaySlot slot = slots.acquire();
        reconcileIndex++;
        if (state == null) {
            presentSlots.clear(index);
            slot.reconcileVisibility(false, result, layerName);
        } else {
            presentSlots.set(index);
            slot.reconcile(state, visible, result, layerName);
        }
    }

    public void finishReconcile(OverlayRefreshResult result) {
        slots.finishReconcile(result, layerName);
        if (reconcileIndex < presentSlots.length()) {
            presentSlots.clear(reconcileIndex, presentSlots.length());
        }
        reconcileIndex = 0;
    }

    public void setVisible(boolean visible, OverlayRefreshResult result) {
        for (int index = 0; index < slots.size(); index++) {
            slots.get(index).reconcileVisibility(visible && presentSlots.get(index), result, layerName);
        }
    }

    public void clear(OverlayRefreshResult result) {
        slots.clear(result, layerName);
        presentSlots.clear();
        reconcileIndex = 0;
    }

    public List<MarkerOverlay> getOverlays() {
        List<MarkerOverlay> overlays = new ArrayList<>(presentSlots.cardinality());
        for (int index = presentSlots.nextSetBit(0); index >= 0; index = presentSlots.nextSetBit(index + 1)) {
            MarkerOverlay overlay = slots.get(index).getOverlay();
            if (overlay != null) {
                overlays.add(overlay);
            }
        }
        return overlays;
    }
}
