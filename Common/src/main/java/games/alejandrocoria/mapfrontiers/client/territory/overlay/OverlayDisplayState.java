package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.IOverlayListener;
import journeymap.api.v2.client.display.Overlay;
import journeymap.api.v2.client.model.TextProperties;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Common JourneyMap overlay values. The text key supplies semantic equality for mutable TextProperties.
 */
public final class OverlayDisplayState {
    private static final int MINIMUM_ZOOM = 2;
    private static final int MAXIMUM_ZOOM = 16384;

    private final ResourceKey<Level> dimension;
    private final OverlayActivation activation;
    private final int minZoom;
    private final int maxZoom;
    private final int displayOrder;
    private final @Nullable String groupName;
    private final @Nullable String title;
    private final @Nullable String label;
    private final @Nullable TextProperties textProperties;
    private final @Nullable Object textPropertiesKey;
    private final @Nullable IOverlayListener listener;

    public OverlayDisplayState(ResourceKey<Level> dimension, OverlayActivation activation, int minZoom, int maxZoom,
                               int displayOrder, @Nullable String groupName, @Nullable String title,
                               @Nullable String label, @Nullable TextProperties textProperties,
                               @Nullable Object textPropertiesKey, @Nullable IOverlayListener listener) {
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.activation = Objects.requireNonNull(activation, "activation");
        this.minZoom = Math.max(MINIMUM_ZOOM, minZoom);
        this.maxZoom = maxZoom > 0 ? Math.min(MAXIMUM_ZOOM, maxZoom) : MAXIMUM_ZOOM;
        this.displayOrder = displayOrder;
        this.groupName = groupName;
        this.title = title;
        this.label = label;
        if ((textProperties == null) != (textPropertiesKey == null)) {
            throw new IllegalArgumentException("Text properties and their semantic key must be supplied together");
        }
        this.textProperties = textProperties;
        this.textPropertiesKey = textPropertiesKey;
        this.listener = listener;
    }

    void applyTo(Overlay overlay) {
        overlay.setDimension(dimension);
        overlay.setActiveUIs(activation.getUis());
        overlay.setActiveMapTypes(activation.getMapTypes());
        overlay.setMinZoom(minZoom);
        overlay.setMaxZoom(maxZoom);
        overlay.setDisplayOrder(displayOrder);
        overlay.setOverlayGroupName(groupName);
        overlay.setTitle(title);
        overlay.setLabel(label);
        overlay.setTextProperties(textProperties);
        overlay.setOverlayListener(listener);
    }

    boolean sameAs(OverlayDisplayState other) {
        return dimension.equals(other.dimension)
                && activation.equals(other.activation)
                && minZoom == other.minZoom
                && maxZoom == other.maxZoom
                && displayOrder == other.displayOrder
                && Objects.equals(groupName, other.groupName)
                && Objects.equals(title, other.title)
                && Objects.equals(label, other.label)
                && Objects.equals(textPropertiesKey, other.textPropertiesKey)
                && listener == other.listener;
    }

    ResourceKey<Level> getDimension() {
        return dimension;
    }
}
