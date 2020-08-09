package games.alejandrocoria.mapfrontiers.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiColors;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.PacketDeleteFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketNewFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketSharePersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ContainerHelper;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.MarkerOverlay;
import journeymap.client.api.model.MapImage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class FrontiersOverlayManager {
    private IClientAPI jmAPI;
    private HashMap<Integer, ArrayList<FrontierOverlay>> dimensionsFrontiers;
    private HashMap<Integer, FrontierOverlay> frontiersSelected;
    private HashMap<Integer, MarkerOverlay> markersSelected;
    private boolean personal;

    private static MapImage markerDotSelected = new MapImage(
            new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"), 20, 0, 10, 10, GuiColors.WHITE, 1.f);
    private static float targetDotSelectedOpacity = 0.3f;

    static {
        markerDotSelected.setAnchorX(markerDotSelected.getDisplayWidth() / 2.0)
                .setAnchorY(markerDotSelected.getDisplayHeight() / 2.0);
        markerDotSelected.setRotation(0);
    }

    public FrontiersOverlayManager(IClientAPI jmAPI, boolean personal) {
        this.jmAPI = jmAPI;
        dimensionsFrontiers = new HashMap<Integer, ArrayList<FrontierOverlay>>();
        frontiersSelected = new HashMap<Integer, FrontierOverlay>();
        markersSelected = new HashMap<Integer, MarkerOverlay>();
        this.personal = personal;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (ClientProxy.hasBookItemInHand()) {
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
        if (jmAPI == null) {
            return null;
        }

        List<FrontierOverlay> frontiers = getAllFrontiers(data.getDimension());
        FrontierOverlay frontierOverlay = new FrontierOverlay(data, jmAPI);
        frontiers.add(frontierOverlay);

        return frontierOverlay;
    }

    public void clientCreateNewfrontier(int dimension) {
        BlockPos vertex = null;

        if (ConfigData.addVertexToNewFrontier) {
            vertex = ((ClientProxy) MapFrontiers.proxy).snapVertex(Minecraft.getMinecraft().player.getPosition(),
                    ConfigData.snapDistance, dimension, null);
        }

        PacketHandler.INSTANCE.sendToServer(new PacketNewFrontier(dimension, personal, vertex));
    }

    public void clientDeleteFrontier(int dimension, int index) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);

        FrontierOverlay frontier = frontiers.get(index);

        if (frontier != null) {
            PacketHandler.INSTANCE.sendToServer(new PacketDeleteFrontier(frontier.getId()));
        }
    }

    public void clientUpdatefrontier(int dimension, int index) {
        clientUpdatefrontier(getAllFrontiers(dimension).get(index));
    }

    public void clientUpdatefrontier(FrontierOverlay frontier) {
        PacketHandler.INSTANCE.sendToServer(new PacketUpdateFrontier(frontier));
        frontier.removeChanges();
    }

    public void clientShareFrontier(UUID frontierID, SettingsUser targetUser) {
        PacketHandler.INSTANCE.sendToServer(new PacketSharePersonalFrontier(frontierID, targetUser));
    }

    public int deleteFrontier(int dimension, UUID id) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(id));

        if (index < 0) {
            return -1;
        }

        FrontierOverlay frontier = frontiers.get(index);
        frontier.removeOverlay();
        frontiers.remove(index);

        return index;
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

    public int getFrontierIndex(FrontierOverlay frontierOverlay) {
        List<FrontierOverlay> frontiers = getAllFrontiers(frontierOverlay.getDimension());
        if (frontiers == null) {
            return -1;
        }

        return frontiers.lastIndexOf(frontierOverlay);
    }

    public Map<Integer, ArrayList<FrontierOverlay>> getAllFrontiers() {
        return dimensionsFrontiers;
    }

    public List<FrontierOverlay> getAllFrontiers(int dimension) {
        ArrayList<FrontierOverlay> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));

        if (frontiers == null) {
            frontiers = new ArrayList<FrontierOverlay>();
            dimensionsFrontiers.put(Integer.valueOf(dimension), frontiers);
        }

        return frontiers;
    }

    public FrontierOverlay getFrontierInPosition(int dimension, BlockPos pos) {
        ArrayList<FrontierOverlay> frontiers = dimensionsFrontiers.get(Integer.valueOf(dimension));
        if (frontiers != null) {
            for (FrontierOverlay frontier : frontiers) {
                if (pos.getX() >= frontier.topLeft.getX() && pos.getX() <= frontier.bottomRight.getX()
                        && pos.getZ() >= frontier.topLeft.getZ() && pos.getZ() <= frontier.bottomRight.getZ()) {
                    if (frontier.pointIsInside(pos)) {
                        return frontier;
                    }
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

    public FrontierOverlay getFrontier(int dimension, int index) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);
        return frontiers.get(index);
    }

    public int getFrontierCount(int dimension) {
        return getAllFrontiers(dimension).size();
    }

    public int getFrontierIndexSelected(int dimension) {
        FrontierOverlay selected = frontiersSelected.get(Integer.valueOf(dimension));
        if (selected == null)
            return -1;

        return getFrontierIndex(selected);
    }

    public void setFrontierIndexSelected(int dimension, int index) {
        if (index < 0) {
            updateSelectedMarker(dimension, null);
        } else {
            List<FrontierOverlay> frontiers = getAllFrontiers(dimension);
            FrontierOverlay frontier = frontiers.get(index);
            updateSelectedMarker(dimension, frontier);
        }
    }

    public void updateSelectedMarker(int dimension, FrontierOverlay frontier) {
        Integer dim = Integer.valueOf(dimension);
        MarkerOverlay marker = markersSelected.get(dim);
        if (marker != null) {
            jmAPI.remove(marker);
        }

        if (frontier != null) {
            frontiersSelected.put(dim, frontier);
            BlockPos pos = frontier.getSelectedVertex();
            if (pos != null) {
                marker = new MarkerOverlay(MapFrontiers.MODID, "selected_vertex_" + String.valueOf(dimension), pos,
                        markerDotSelected);
                marker.setDimension(dimension);
                marker.setDisplayOrder(101);

                try {
                    jmAPI.show(marker);
                    markersSelected.put(dim, marker);
                } catch (Throwable t) {
                    MapFrontiers.LOGGER.error(t.getMessage(), t);
                }
            }
        } else {
            frontiersSelected.remove(dim);
        }
    }
}
