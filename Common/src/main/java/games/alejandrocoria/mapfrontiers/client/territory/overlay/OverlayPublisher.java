package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.Displayable;

public interface OverlayPublisher {
    boolean isAvailable();

    void show(Displayable overlay) throws Exception;

    void remove(Displayable overlay);
}
