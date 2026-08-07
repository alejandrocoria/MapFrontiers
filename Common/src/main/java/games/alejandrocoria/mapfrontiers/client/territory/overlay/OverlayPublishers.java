package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Displayable;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

public final class OverlayPublishers {
    private static final OverlayPublisher UNAVAILABLE = new OverlayPublisher() {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void show(Displayable overlay) {
        }

        @Override
        public void remove(Displayable overlay) {
        }
    };

    private OverlayPublishers() {
    }

    public static OverlayPublisher create(@Nullable IClientAPI journeyMapApi) {
        return journeyMapApi == null ? UNAVAILABLE : new JourneyMapOverlayPublisher(journeyMapApi);
    }

    private record JourneyMapOverlayPublisher(IClientAPI journeyMapApi) implements OverlayPublisher {
        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void show(Displayable overlay) throws Exception {
            assert Minecraft.getInstance().isSameThread() : "JourneyMap overlays must be shown on the client thread";
            journeyMapApi.show(overlay);
        }

        @Override
        public void remove(Displayable overlay) {
            assert Minecraft.getInstance().isSameThread() : "JourneyMap overlays must be removed on the client thread";
            journeyMapApi.remove(overlay);
        }
    }
}
