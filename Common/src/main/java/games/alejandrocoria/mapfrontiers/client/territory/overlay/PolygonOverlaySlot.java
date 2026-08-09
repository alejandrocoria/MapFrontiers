package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.PolygonOverlay;

import javax.annotation.Nullable;
import java.util.Objects;

public final class PolygonOverlaySlot extends OverlaySlot<PolygonOverlay> {
    private final String modId;
    private @Nullable PolygonOverlayState desiredState;

    public PolygonOverlaySlot(String modId, OverlayPublisher publisher) {
        super(publisher);
        this.modId = Objects.requireNonNull(modId, "modId");
    }

    public void reconcile(PolygonOverlayState state, boolean visible, OverlayRefreshResult result, String layer) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(layer, "layer");

        boolean stateChanged = desiredState == null || !desiredState.sameAs(state);
        PolygonOverlay overlay = getOverlay();
        if (overlay == null) {
            overlay = new PolygonOverlay(modId, state.getDisplayState().getDimension(), state.getShapeProperties(),
                    state.getOuterArea(), state.getHoles());
            initializeOverlay(overlay);
            stateChanged = true;
        }

        if (stateChanged) {
            overlay.setOuterArea(state.getOuterArea());
            overlay.setHoles(state.getHoles());
            overlay.setShapeProperties(state.getShapeProperties());
            // JourneyMap's polygon renderer always dereferences TextProperties, even without a label.
            state.getDisplayState().applyTo(overlay, false);
            desiredState = state;
        }

        reconcilePublication(visible, stateChanged, result, layer);
    }

    @Override
    protected void clearDesiredState() {
        desiredState = null;
    }
}
