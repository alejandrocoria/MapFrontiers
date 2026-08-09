package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Objects;

public final class MarkerOverlaySlot extends OverlaySlot<MarkerOverlay> {
    private final String modId;
    private @Nullable BlockPos desiredPoint;
    private @Nullable Object desiredVisualKey;
    private @Nullable OverlayDisplayState desiredDisplayState;

    public MarkerOverlaySlot(String modId, OverlayPublisher publisher) {
        super(publisher);
        this.modId = Objects.requireNonNull(modId, "modId");
    }

    public void reconcile(MarkerOverlayState state, boolean visible, OverlayRefreshResult result, String layer) {
        Objects.requireNonNull(state, "state");
        reconcile(state.getPoint(), state.getIcon(), state.getVisualKey(), state.getDisplayState(), visible, result, layer);
    }

    public void reconcile(BlockPos point, MapImage icon, Object visualKey, OverlayDisplayState displayState,
                          boolean visible, OverlayRefreshResult result, String layer) {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(visualKey, "visualKey");
        Objects.requireNonNull(displayState, "displayState");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(layer, "layer");

        boolean pointChanged = desiredPoint == null || !desiredPoint.equals(point);
        boolean visualChanged = !Objects.equals(desiredVisualKey, visualKey);
        boolean displayStateChanged = desiredDisplayState == null || !desiredDisplayState.sameAs(displayState);
        MarkerOverlay overlay = getOverlay();
        if (overlay == null) {
            overlay = new MarkerOverlay(modId, point, icon);
            initializeOverlay(overlay);
            pointChanged = true;
            visualChanged = true;
            displayStateChanged = true;
        }

        if (pointChanged) {
            overlay.setPoint(point);
            desiredPoint = point;
        }
        if (visualChanged) {
            overlay.setIcon(icon);
            desiredVisualKey = visualKey;
        }
        if (displayStateChanged) {
            // JourneyMap's marker renderer dereferences TextProperties even when the marker has no label.
            boolean resetNeutralTextProperties = desiredDisplayState != null
                    && desiredDisplayState.hasTextProperties()
                    && !displayState.hasTextProperties();
            displayState.applyTo(overlay, resetNeutralTextProperties);
            desiredDisplayState = displayState;
        }

        boolean stateChanged = pointChanged || visualChanged || displayStateChanged;
        reconcilePublication(visible, stateChanged, result, layer);
    }

    @Override
    protected void clearDesiredState() {
        desiredPoint = null;
        desiredVisualKey = null;
        desiredDisplayState = null;
    }
}
