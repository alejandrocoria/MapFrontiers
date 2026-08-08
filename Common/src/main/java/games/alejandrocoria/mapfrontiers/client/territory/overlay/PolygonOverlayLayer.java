package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.PolygonOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reconciles one ordered polygon layer while preserving overlay identity between refreshes.
 */
public final class PolygonOverlayLayer {
    private final String layerName;
    private final ReconciledOverlayList<PolygonOverlaySlot> slots;

    public PolygonOverlayLayer(String modId, String layerName, OverlayPublisher publisher) {
        Objects.requireNonNull(modId, "modId");
        this.layerName = Objects.requireNonNull(layerName, "layerName");
        Objects.requireNonNull(publisher, "publisher");
        slots = new ReconciledOverlayList<>(() -> new PolygonOverlaySlot(modId, publisher));
    }

    public void beginReconcile() {
        slots.beginReconcile();
    }

    public void reconcileNext(PolygonOverlayState state, boolean visible, OverlayRefreshResult result) {
        slots.acquire().reconcile(state, visible, result, layerName);
    }

    public void finishReconcile(OverlayRefreshResult result) {
        slots.finishReconcile(result, layerName);
    }

    public void setVisible(boolean visible, OverlayRefreshResult result) {
        for (int index = 0; index < slots.size(); index++) {
            slots.get(index).reconcileVisibility(visible, result, layerName);
        }
    }

    public void clear(OverlayRefreshResult result) {
        slots.clear(result, layerName);
    }

    public List<PolygonOverlay> getOverlays() {
        List<PolygonOverlay> overlays = new ArrayList<>(slots.size());
        for (int index = 0; index < slots.size(); index++) {
            PolygonOverlay overlay = slots.get(index).getOverlay();
            if (overlay != null) {
                overlays.add(overlay);
            }
        }
        return overlays;
    }
}
