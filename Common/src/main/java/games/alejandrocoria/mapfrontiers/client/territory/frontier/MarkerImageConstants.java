package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import journeymap.api.v2.client.model.MapImage;

public final class MarkerImageConstants {
    public static final int TEXTURE_SIZE = 60;
    public static final int BASE_DISPLAY_SIZE = 12;
    public static final int SELECTOR_DISPLAY_SIZE = 12;

    public static int getMapDisplaySize() {
        return BASE_DISPLAY_SIZE * ClientConfig.PATH_MARKER_SIZE.get();
    }

    public static void applyMapDisplaySize(MapImage marker) {
        int displaySize = getMapDisplaySize();
        marker.setDisplayWidth(displaySize);
        marker.setDisplayHeight(displaySize);
        marker.setAnchorX(displaySize / 2.0).setAnchorY(displaySize / 2.0);
    }

    private MarkerImageConstants() {
    }
}
