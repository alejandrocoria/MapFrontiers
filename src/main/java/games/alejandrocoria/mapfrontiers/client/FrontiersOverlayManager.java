package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiColors;
import games.alejandrocoria.mapfrontiers.client.plugin.MapFrontiersPlugin;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.*;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ContainerHelper;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.MarkerOverlay;
import journeymap.client.api.model.MapImage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class FrontiersOverlayManager {
    private final IClientAPI jmAPI;
    private final HashMap<ResourceKey<Level>, ArrayList<FrontierOverlay>> dimensionsFrontiers;
    private final HashMap<ResourceKey<Level>, MarkerOverlay> markersSelected;
    private final boolean personal;

    private static final MapImage markerDotSelected = new MapImage(
            new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"), 20, 0, 10, 10, GuiColors.WHITE, 1.f);
    private static float targetDotSelectedOpacity = 0.3f;

    static {
        markerDotSelected.setAnchorX(markerDotSelected.getDisplayWidth() / 2.0)
                .setAnchorY(markerDotSelected.getDisplayHeight() / 2.0);
        markerDotSelected.setRotation(0);
    }

    public FrontiersOverlayManager(IClientAPI jmAPI, boolean personal) {
        this.jmAPI = jmAPI;
        dimensionsFrontiers = new HashMap<>();
        markersSelected = new HashMap<>();
        this.personal = personal;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (MapFrontiersPlugin.isEditing()) {
                float opacity = markerDotSelected.getOpacity();
                if (opacity < targetDotSelectedOpacity) {
                    opacity += event.renderTickTime * 0.5f;
                    if (opacity >= targetDotSelectedOpacity) {
                        opacity = targetDotSelectedOpacity;
                        targetDotSelectedOpacity = 0.f;
                    }
                } else {
                    opacity -= event.renderTickTime * 0.07f;
                    if (opacity <= targetDotSelectedOpacity) {
                        opacity = targetDotSelectedOpacity;
                        targetDotSelectedOpacity = 1.f;
                    }
                }
                markerDotSelected.setOpacity(opacity);
            } else {
                markerDotSelected.setOpacity(0.f);
                targetDotSelectedOpacity = 1.f;
            }
        }
    }

    public FrontierOverlay addFrontier(FrontierData data) {
        List<FrontierOverlay> frontiers = getAllFrontiers(data.getDimension());
        FrontierOverlay frontierOverlay = new FrontierOverlay(data, jmAPI);
        frontiers.add(frontierOverlay);

        return frontierOverlay;
    }

    public void clientCreateNewfrontier(ResourceKey<Level> dimension, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        PacketHandler.INSTANCE.sendToServer(new PacketNewFrontier(dimension, personal, vertices, chunks));
    }

    public void clientDeleteFrontier(FrontierOverlay frontier) {
        PacketHandler.INSTANCE.sendToServer(new PacketDeleteFrontier(frontier.getId()));
    }

    public void clientUpdatefrontier(FrontierOverlay frontier) {
        PacketHandler.INSTANCE.sendToServer(new PacketUpdateFrontier(frontier));
        frontier.removeChanges();
    }

    public void clientShareFrontier(UUID frontierID, SettingsUser targetUser) {
        PacketHandler.INSTANCE.sendToServer(new PacketSharePersonalFrontier(frontierID, targetUser));
    }

    public boolean deleteFrontier(ResourceKey<Level> dimension, UUID id) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(id));

        if (index < 0) {
            return false;
        }

        FrontierOverlay frontier = frontiers.get(index);
        frontier.removeOverlay();
        frontiers.remove(index);

        return true;
    }

    public FrontierOverlay updateFrontier(FrontierData data) {
        List<FrontierOverlay> frontiers = getAllFrontiers(data.getDimension());

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(data.getId()));

        if (index < 0) {
            return null;
        } else {
            FrontierOverlay frontierOverlay = frontiers.get(index);
            frontierOverlay.updateFromData(data);

            return frontierOverlay;
        }
    }

    public Map<ResourceKey<Level>, ArrayList<FrontierOverlay>> getAllFrontiers() {
        return dimensionsFrontiers;
    }

    public List<FrontierOverlay> getAllFrontiers(ResourceKey<Level> dimension) {
        return dimensionsFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public FrontierOverlay getFrontierInPosition(ResourceKey<Level> dimension, BlockPos pos) {
        return getFrontierInPosition(dimension, pos, 0.0);
    }

    public FrontierOverlay getFrontierInPosition(ResourceKey<Level> dimension, BlockPos pos, double maxDistanceToOpen) {
        ArrayList<FrontierOverlay> frontiers = dimensionsFrontiers.get(dimension);
        if (frontiers != null) {
            for (FrontierOverlay frontier : frontiers) {
                if (frontier.pointIsInside(pos, maxDistanceToOpen)) {
                    return frontier;
                }
            }
        }

        return null;
    }

    public void updateAllOverlays() {
        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                frontier.updateOverlay();
            }
        }
    }

    public void removeAllOverlays() {
        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                frontier.removeOverlay();
            }
        }
    }

    public void updateSelectedMarker(ResourceKey<Level> dimension, @Nullable FrontierOverlay frontier) {
        MarkerOverlay marker = markersSelected.get(dimension);
        if (marker != null) {
            jmAPI.remove(marker);
        }

        if (frontier != null) {
            BlockPos pos = frontier.getSelectedVertex();
            if (pos != null) {
                marker = new MarkerOverlay(MapFrontiers.MODID, "selected_vertex_" + dimension.location(), pos,
                        markerDotSelected);
                marker.setDimension(dimension);
                marker.setDisplayOrder(101);

                try {
                    jmAPI.show(marker);
                    markersSelected.put(dimension, marker);
                } catch (Throwable t) {
                    MapFrontiers.LOGGER.error(t.getMessage(), t);
                }
            }
        }
    }
}
