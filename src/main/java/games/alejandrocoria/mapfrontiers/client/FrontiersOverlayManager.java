package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiColors;
import games.alejandrocoria.mapfrontiers.client.plugin.MapFrontiersPlugin;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.*;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.common.util.ContainerHelper;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.MarkerOverlay;
import journeymap.client.api.model.MapImage;
import journeymap.client.io.FileHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class FrontiersOverlayManager {
    private final IClientAPI jmAPI;
    private final HashMap<RegistryKey<World>, ArrayList<FrontierOverlay>> dimensionsFrontiers;
    private final HashMap<RegistryKey<World>, MarkerOverlay> markersSelected;
    private final boolean personal;
    private File ModDir;
    public static final int dataVersion = 9;

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

    public void setFrontiersFromServer(List<FrontierData> frontiers) {
        ensureLoadData();

        if (personal && !Minecraft.getInstance().isLocalServer()) {
            List<FrontierOverlay> localFrontiers = new ArrayList<>();
            dimensionsFrontiers.values().forEach(localFrontiers::addAll);

            for (FrontierData data : frontiers) {
                if (localFrontiers.removeIf(x -> x.getId().equals(data.getId()))) {
                    FrontierOverlay frontierOverlay = getAllFrontiers(data.getDimension()).stream().filter(x -> x.getId().equals(data.getId())).findFirst().orElseThrow(null);
                    frontierOverlay.updateFromData(data);
                } else {
                    FrontierOverlay frontierOverlay = new FrontierOverlay(data, jmAPI);
                    getAllFrontiers(data.getDimension()).add(frontierOverlay);
                }
            }

            saveData();

            SettingsUser playerUser = new SettingsUser(Minecraft.getInstance().player);
            for (FrontierOverlay frontier : localFrontiers) {
                if (frontier.getOwner().equals(playerUser)) {
                    frontier.removeAllUserShared();
                    PacketHandler.INSTANCE.sendToServer(new PacketPersonalFrontier(frontier));
                    frontier.removeChanges();
                }
            }
        } else {
            for (FrontierData data : frontiers) {
                FrontierOverlay frontierOverlay = new FrontierOverlay(data, jmAPI);
                getAllFrontiers(data.getDimension()).add(frontierOverlay);
            }
        }
    }

    public FrontierOverlay addFrontier(FrontierData data) {
        List<FrontierOverlay> frontiers = getAllFrontiers(data.getDimension());

        FrontierOverlay frontierOverlay = new FrontierOverlay(data, jmAPI);
        frontiers.add(frontierOverlay);

        if (personal && !Minecraft.getInstance().isLocalServer()) {
            saveData();
        }

        return frontierOverlay;
    }

    public void clientCreateNewfrontier(RegistryKey<World> dimension, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        if (ClientProxy.isModOnServer()) {
            PacketHandler.INSTANCE.sendToServer(new PacketCreateFrontier(dimension, personal, vertices, chunks));
        } else if (personal) {
            FrontierData frontier = new FrontierData();
            frontier.setId(UUID.randomUUID());
            frontier.setOwner(new SettingsUser(Minecraft.getInstance().player));
            frontier.setDimension(dimension);
            frontier.setPersonal(true);
            frontier.setColor(ColorHelper.getRandomColor());
            frontier.setCreated(new Date());

            if (vertices != null) {
                frontier.setMode(FrontierData.Mode.Vertex);
                for (BlockPos vertex : vertices) {
                    frontier.addVertex(vertex);
                }
            }

            if (chunks != null) {
                frontier.setMode(FrontierData.Mode.Chunk);
                for (ChunkPos chunk : chunks) {
                    frontier.toggleChunk(chunk);
                }
            }

            addFrontier(frontier);
        }
    }

    public void clientDeleteFrontier(FrontierOverlay frontier) {
        if (ClientProxy.isModOnServer()) {
            PacketHandler.INSTANCE.sendToServer(new PacketDeleteFrontier(frontier.getId()));
        } else if (personal && frontier.getOwner().equals(new SettingsUser(Minecraft.getInstance().player))) {
            deleteFrontier(frontier.getDimension(), frontier.getId());
        }
    }

    public void clientUpdatefrontier(FrontierOverlay frontier) {
        if (ClientProxy.isModOnServer()) {
            PacketHandler.INSTANCE.sendToServer(new PacketUpdateFrontier(frontier));
            frontier.removeChanges();
        } else if (personal && frontier.getOwner().equals(new SettingsUser(Minecraft.getInstance().player))) {
            updateFrontier(frontier);
        }
    }

    public void clientShareFrontier(UUID frontierID, SettingsUser targetUser) {
        if (ClientProxy.isModOnServer()) {
            PacketHandler.INSTANCE.sendToServer(new PacketSharePersonalFrontier(frontierID, targetUser));
        }
    }

    public boolean deleteFrontier(RegistryKey<World> dimension, UUID id) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(id));

        if (index < 0) {
            return false;
        }

        FrontierOverlay frontier = frontiers.get(index);
        frontier.removeOverlay();
        frontiers.remove(index);

        if (personal && !Minecraft.getInstance().isLocalServer()) {
            saveData();
        }

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

            if (personal && !Minecraft.getInstance().isLocalServer()) {
                saveData();
            }

            return frontierOverlay;
        }
    }

    public Map<RegistryKey<World>, ArrayList<FrontierOverlay>> getAllFrontiers() {
        ensureLoadData();
        return dimensionsFrontiers;
    }

    public List<FrontierOverlay> getAllFrontiers(RegistryKey<World> dimension) {
        ensureLoadData();
        return dimensionsFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public FrontierOverlay getFrontierInPosition(RegistryKey<World> dimension, BlockPos pos) {
        return getFrontierInPosition(dimension, pos, 0.0);
    }

    public FrontierOverlay getFrontierInPosition(RegistryKey<World> dimension, BlockPos pos, double maxDistanceToOpen) {
        ArrayList<FrontierOverlay> frontiers = dimensionsFrontiers.get(dimension);
        if (frontiers != null) {
            for (FrontierOverlay frontier : frontiers) {
                if (frontier.getVisible() && frontier.pointIsInside(pos, maxDistanceToOpen)) {
                    return frontier;
                }
            }
        }

        return null;
    }

    public Set<FrontierOverlay> getFrontiersForAnnounce(RegistryKey<World> dimension, BlockPos pos) {
        Set<FrontierOverlay> inPosition = new HashSet<>();
        ArrayList<FrontierOverlay> frontiers = dimensionsFrontiers.get(dimension);
        if (frontiers != null) {
            for (FrontierOverlay frontier : frontiers) {
                if (frontier.getAnnounceInChat() && frontier.pointIsInside(pos, 0.0)) {
                    inPosition.add(frontier);
                }
            }
        }

        return inPosition;
    }

    public void updateAllOverlays(boolean forceUpdate) {
        ensureLoadData();

        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                if (forceUpdate) {
                    frontier.updateOverlay();
                } else {
                    frontier.updateOverlayIfNeeded();
                }
            }
        }
    }

    public void removeAllOverlays() {
        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                frontier.removeOverlay();
            }
        }

        dimensionsFrontiers.clear();

        ModDir = null;
    }

    public void updateSelectedMarker(RegistryKey<World> dimension, @Nullable FrontierOverlay frontier) {
        MarkerOverlay marker = markersSelected.get(dimension);
        if (marker != null) {
            jmAPI.remove(marker);
        }

        if (frontier != null) {
            BlockPos pos = frontier.getSelectedVertex();
            if (pos != null) {
                marker = new MarkerOverlay(MapFrontiers.MODID, "selected_vertex_" + dimension.location(), pos, markerDotSelected);
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

    private void readFromNBT(CompoundNBT nbt) {
        int version = nbt.getInt("Version");
        if (version == 0) {
            MapFrontiers.LOGGER.warn("Data version in personal_frontiers not found, expected " + dataVersion);
        } else if (version > dataVersion) {
            MapFrontiers.LOGGER.warn("Data version in personal_frontiers higher than expected. The mod uses " + dataVersion);
        }

        ListNBT frontiersTagList = nbt.getList("frontiers", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < frontiersTagList.size(); ++i) {
            FrontierData frontier = new FrontierData();
            CompoundNBT frontierTag = frontiersTagList.getCompound(i);
            frontier.readFromNBT(frontierTag, version);
            List<FrontierOverlay> frontiers = getAllFrontiers(frontier.getDimension());
            FrontierOverlay frontierOverlay = new FrontierOverlay(frontier, jmAPI);
            frontiers.add(frontierOverlay);
        }
    }

    private void writeToNBT(CompoundNBT nbt) {
        SettingsUser playerUser = new SettingsUser(Minecraft.getInstance().player);
        ListNBT frontiersTagList = new ListNBT();
        for (Map.Entry<RegistryKey<World>, ArrayList<FrontierOverlay>> frontiers : dimensionsFrontiers.entrySet()) {
            for (FrontierData frontier : frontiers.getValue()) {
                if (frontier.getPersonal() && frontier.getOwner().equals(playerUser)) {
                    CompoundNBT frontierTag = new CompoundNBT();
                    frontier.writeToNBT(frontierTag);
                    frontiersTagList.add(frontierTag);
                }
            }
        }
        nbt.put("frontiers", frontiersTagList);

        nbt.putInt("Version", dataVersion);
    }

    private void ensureLoadData() {
        if (personal && !Minecraft.getInstance().isLocalServer() && ModDir == null) {
            loadData();
        }
    }

    private void loadData() {
        try {
            File jmDir = FileHandler.getJMWorldDir(Minecraft.getInstance());
            ModDir = new File(jmDir, "mapfrontier");
            //noinspection ResultOfMethodCallIgnored
            ModDir.mkdirs();

            CompoundNBT nbtFrontiers = loadFile("personal_frontiers.dat");
            if (!nbtFrontiers.isEmpty()) {
                readFromNBT(nbtFrontiers);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    private void saveData() {
        if (personal && !Minecraft.getInstance().isLocalServer() && ModDir != null) {
            CompoundNBT nbtFrontiers = new CompoundNBT();
            writeToNBT(nbtFrontiers);
            saveFile("personal_frontiers.dat", nbtFrontiers);
        }
    }

    private CompoundNBT loadFile(String filename) {
        File f = new File(ModDir, filename);
        if (f.exists()) {
            try (FileInputStream inputStream = new FileInputStream(f)) {
                return CompressedStreamTools.readCompressed(inputStream);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        }

        return new CompoundNBT();
    }

    private void saveFile(String filename, CompoundNBT nbt) {
        try {
            File f = new File(ModDir, filename);
            try (FileOutputStream outputStream = new FileOutputStream(f)) {
                CompressedStreamTools.writeCompressed(nbt, outputStream);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }
}
