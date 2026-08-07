package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.model.MapImage;
import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Desired marker values. The visual key, rather than MapImage identity, defines whether the icon changed.
 */
public final class MarkerOverlayState {
    private final BlockPos point;
    private final MapImage icon;
    private final Object visualKey;
    private final OverlayDisplayState displayState;

    public MarkerOverlayState(BlockPos point, MapImage icon, Object visualKey, OverlayDisplayState displayState) {
        this.point = Objects.requireNonNull(point, "point");
        this.icon = Objects.requireNonNull(icon, "icon");
        this.visualKey = Objects.requireNonNull(visualKey, "visualKey");
        this.displayState = Objects.requireNonNull(displayState, "displayState");
    }

    BlockPos getPoint() {
        return point;
    }

    MapImage getIcon() {
        return icon;
    }

    Object getVisualKey() {
        return visualKey;
    }

    OverlayDisplayState getDisplayState() {
        return displayState;
    }
}
