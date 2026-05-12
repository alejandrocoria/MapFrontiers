package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class SelectedEditablePointMarker {
    private static final String[] FRAME_TEXTURES = {
            "textures/markers/selected/0.png",
            "textures/markers/selected/1.png",
            "textures/markers/selected/2.png",
            "textures/markers/selected/3.png",
            "textures/markers/selected/4.png",
            "textures/markers/selected/5.png",
            "textures/markers/selected/6.png",
            "textures/markers/selected/7.png",
            "textures/markers/selected/8.png"};
    private static final float FRAME_TICKS = 0.25f;
    private static final int DISPLAY_ORDER = 101;

    private final IClientAPI jmAPI;
    private final MapImage[] frameImages;
    private final Map<ResourceKey<Level>, MarkerOverlay[]> overlaysByDimension = new HashMap<>();
    private int activeFrame = 0;
    private float animationTicks = 0.f;
    private boolean visible = false;

    public SelectedEditablePointMarker(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
        frameImages = createFrameImages();
        updateFrameOpacity();
    }

    public void tick(float deltaTicks, boolean visible) {
        this.visible = visible;

        if (!visible) {
            animationTicks = 0.f;
            activeFrame = 0;
            updateFrameOpacity();
            return;
        }

        animationTicks += deltaTicks;
        int nextFrame = (int) (animationTicks / FRAME_TICKS) % frameImages.length;
        if (nextFrame != activeFrame) {
            activeFrame = nextFrame;
            updateFrameOpacity();
        } else if (frameImages[activeFrame].getOpacity() == 0.f) {
            updateFrameOpacity();
        }
    }

    public void update(ResourceKey<Level> dimension, @Nullable BlockPos pos) {
        remove(dimension);

        if (pos == null) {
            return;
        }

        MarkerOverlay[] overlays = createOverlays(dimension, pos);
        try {
            show(overlays);
            overlaysByDimension.put(dimension, overlays);
        } catch (Throwable t) {
            remove(overlays);
            MapFrontiers.LOGGER.error(t.getMessage(), t);
        }
    }

    public void clear() {
        try {
            for (MarkerOverlay[] overlays : overlaysByDimension.values()) {
                remove(overlays);
            }
        } finally {
            overlaysByDimension.clear();
        }
    }

    public void configUpdated() {
        for (MapImage frameImage : frameImages) {
            MarkerImageConstants.applyMapDisplaySize(frameImage);
        }
    }

    private void remove(ResourceKey<Level> dimension) {
        MarkerOverlay[] overlays = overlaysByDimension.remove(dimension);
        if (overlays != null) {
            remove(overlays);
        }
    }

    private static MapImage[] createFrameImages() {
        MapImage[] images = new MapImage[FRAME_TEXTURES.length];
        for (int i = 0; i < FRAME_TEXTURES.length; ++i) {
            images[i] = createFrameImage(FRAME_TEXTURES[i]);
        }
        return images;
    }

    private static MapImage createFrameImage(String texturePath) {
        MapImage marker = new MapImage(
                Identifier.fromNamespaceAndPath(MapFrontiers.MODID, texturePath), 0, 0,
                MarkerImageConstants.TEXTURE_SIZE, MarkerImageConstants.TEXTURE_SIZE, ColorConstants.WHITE, 0.f);
        MarkerImageConstants.applyMapDisplaySize(marker);
        marker.setRotation(0);
        return marker;
    }

    private MarkerOverlay[] createOverlays(ResourceKey<Level> dimension, BlockPos pos) {
        MarkerOverlay[] overlays = new MarkerOverlay[frameImages.length];
        for (int i = 0; i < frameImages.length; ++i) {
            overlays[i] = createOverlay(dimension, pos, frameImages[i]);
        }
        return overlays;
    }

    private static MarkerOverlay createOverlay(ResourceKey<Level> dimension, BlockPos pos, MapImage image) {
        MarkerOverlay marker = new MarkerOverlay(MapFrontiers.MODID, pos, image);
        marker.setDimension(dimension);
        marker.setDisplayOrder(DISPLAY_ORDER);
        return marker;
    }

    private void updateFrameOpacity() {
        for (int i = 0; i < frameImages.length; ++i) {
            frameImages[i].setOpacity(visible && i == activeFrame ? 1.f : 0.f);
        }
    }

    private void show(MarkerOverlay[] overlays) throws Exception {
        for (MarkerOverlay overlay : overlays) {
            jmAPI.show(overlay);
        }
    }

    private void remove(MarkerOverlay[] overlays) {
        for (MarkerOverlay overlay : overlays) {
            try {
                jmAPI.remove(overlay);
            } catch (Throwable t) {
                MapFrontiers.LOGGER.error("Failed to remove selected frontier marker", t);
            }
        }
    }
}
