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

        boolean stateChanged = desiredPoint == null
                || !desiredPoint.equals(point)
                || !Objects.equals(desiredVisualKey, visualKey)
                || desiredDisplayState == null
                || !desiredDisplayState.sameAs(displayState);
        MarkerOverlay overlay = getOverlay();
        if (overlay == null) {
            overlay = new MarkerOverlay(modId, point, icon);
            initializeOverlay(overlay);
            stateChanged = true;
        }

        if (stateChanged) {
            overlay.setPoint(point);
            overlay.setIcon(icon);
            displayState.applyTo(overlay);
            desiredPoint = point;
            desiredVisualKey = visualKey;
            desiredDisplayState = displayState;
        }

        reconcilePublication(visible, stateChanged, result, layer);
    }

    @Override
    protected void clearDesiredState() {
        desiredPoint = null;
        desiredVisualKey = null;
        desiredDisplayState = null;
    }
}
