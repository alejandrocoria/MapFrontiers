package games.alejandrocoria.mapfrontiers.client;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.api.model.ChunkCoord;
import games.alejandrocoria.mapfrontiers.api.model.FrontierShape;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.plugin.MapFrontiersPlugin;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.PacketCreateFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketDeleteFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketPersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketSharePersonalFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketUpdateFrontier;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.common.util.ContainerHelper;
import games.alejandrocoria.mapfrontiers.platform.Services;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class FrontiersOverlayManager {
    private final IClientAPI jmAPI;
    private final HashMap<ResourceKey<Level>, ArrayList<FrontierOverlay>> dimensionsFrontiers;
    private final HashMap<ResourceKey<Level>, MarkerOverlay> markersSelected;
    private final boolean personal;
    private File ModDir;
    private static final Minecraft minecraft = Minecraft.getInstance();

    private static final MapImage markerDotSelected = new MapImage(
            ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/marker.png"), 20, 0, 10, 10, ColorConstants.WHITE, 1.f);
    private static float targetDotSelectedOpacity = 0.3f;

    static {
        markerDotSelected.setAnchorX(markerDotSelected.getDisplayWidth() / 2.0)
                .setAnchorY(markerDotSelected.getDisplayHeight() / 2.0);
        markerDotSelected.setRotation(0);

        ClientEventHandler.subscribeClientTickEvent(FrontiersOverlayManager.class, client -> {
            if (MapFrontiersPlugin.isEditing()) {
                float opacity = markerDotSelected.getOpacity();
                if (opacity < targetDotSelectedOpacity) {
                    opacity += client.getTimer().getGameTimeDeltaTicks() * 0.5f;
                    if (opacity >= targetDotSelectedOpacity) {
                        opacity = targetDotSelectedOpacity;
                        targetDotSelectedOpacity = 0.f;
                    }
                } else {
                    opacity -= client.getTimer().getGameTimeDeltaTicks() * 0.07f;
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
        });
    }

    public FrontiersOverlayManager(IClientAPI jmAPI, boolean personal) {
        this.jmAPI = jmAPI;
        dimensionsFrontiers = new HashMap<>();
        markersSelected = new HashMap<>();
        this.personal = personal;

        ClientEventHandler.subscribeUpdatedConfigEvent(this, () -> updateAllOverlays(true));
    }

    public void close() {
        ClientEventHandler.unsubscribeAllEvents(this);

        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                frontier.deleted();
            }
        }

        dimensionsFrontiers.clear();
        ModDir = null;
    }

    public void setFrontiersFromServer(List<FrontierData> frontiers) {
        ensureLoadData();

        if (personal && !minecraft.isLocalServer()) {
            List<FrontierOverlay> localFrontiers = new ArrayList<>();
            dimensionsFrontiers.values().forEach(localFrontiers::addAll);

            for (FrontierData data : frontiers) {
                if (localFrontiers.removeIf(x -> x.getId().equals(data.getId()))) {
                    FrontierOverlay frontierOverlay = getAllFrontiers(data.getDimension()).stream().filter(x -> x.getId().equals(data.getId())).findFirst().orElseThrow();
                    frontierOverlay.updateFromData(data);
                } else {
                    FrontierOverlay frontierOverlay = new FrontierOverlay(data, jmAPI);
                    getAllFrontiers(data.getDimension()).add(frontierOverlay);
                }
            }

            saveData();

            if (minecraft.player != null) {
                SettingsUser playerUser = new SettingsUser(minecraft.player);
                for (FrontierOverlay frontier : localFrontiers) {
                    if (frontier.getOwner().equals(playerUser)) {
                        frontier.removeAllUserShared();
                        PacketHandler.sendToServer(new PacketPersonalFrontier(frontier));
                        frontier.removeChanges();
                    }
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

        if (personal && !minecraft.isLocalServer()) {
            saveData();
        }

        return frontierOverlay;
    }

    public void addFrontier(FrontierOverlay frontierOverlay) {
        List<FrontierOverlay> frontiers = getAllFrontiers(frontierOverlay.getDimension());
        frontiers.add(frontierOverlay);

        if (personal && !minecraft.isLocalServer()) {
            saveData();
        }
    }

    public void clientCreateNewFrontier(ResourceKey<Level> dimension, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        clientCreateNewFrontierAndReturn(UUID.randomUUID(), dimension, vertices, chunks);
    }

    @Nullable
    public FrontierOverlay clientCreateNewFrontierAndReturn(UUID frontierId, ResourceKey<Level> dimension, @Nullable String sourcePluginId, FrontierShape shape) {
        List<Point2i> shapeVertices = shape.vertices();
        List<ChunkCoord> shapeChunks = shape.chunks();
        List<BlockPos> vertices = shapeVertices == null || shapeVertices.isEmpty() ? null : shapeVertices.stream().map(vertex -> new BlockPos(vertex.x(), 0, vertex.z())).toList();
        List<ChunkPos> chunks = shapeChunks == null || shapeChunks.isEmpty() ? null : shapeChunks.stream().map(chunk -> new ChunkPos(chunk.x(), chunk.z())).toList();
        return clientCreateNewFrontierAndReturn(frontierId, dimension, sourcePluginId, vertices, chunks);
    }

    @Nullable
    public FrontierOverlay clientCreateNewFrontierAndReturn(UUID frontierId, ResourceKey<Level> dimension, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        return clientCreateNewFrontierAndReturn(frontierId, dimension, null, vertices, chunks);
    }

    @Nullable
    public FrontierOverlay clientCreateNewFrontierAndReturn(UUID frontierId,
                                                            ResourceKey<Level> dimension,
                                                            @Nullable String sourcePluginId,
                                                            @Nullable List<BlockPos> vertices,
                                                            @Nullable List<ChunkPos> chunks) {
        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketCreateFrontier(frontierId, dimension, personal, sourcePluginId, vertices, chunks));
            return null;
        } else if (personal && minecraft.player != null) {
            FrontierData frontier = new FrontierData();
            frontier.setId(frontierId);
            frontier.setOwner(new SettingsUser(minecraft.player));
            frontier.setDimension(dimension);
            frontier.setPersonal(true);
            frontier.setSourcePluginId(sourcePluginId);
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

            FrontierOverlay frontierOverlay = addFrontier(frontier);

            ClientEventHandler.postNewFrontierEvent(frontierOverlay, minecraft.player.getId());
            return frontierOverlay;
        }

        return null;
    }

    public void clientDeleteFrontier(FrontierOverlay frontier) {
        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketDeleteFrontier(frontier.getId()));
        } else if (personal && minecraft.player != null && frontier.getOwner().equals(new SettingsUser(minecraft.player))) {
            deleteFrontier(frontier.getDimension(), frontier.getId());
            ClientEventHandler.postDeletedFrontierEvent(frontier.getId());
        }
    }

    public void clientUpdateFrontier(FrontierOverlay frontier) {
        if (MapFrontiersClient.isModOnServer()) {
            PacketHandler.sendToServer(new PacketUpdateFrontier(frontier));
            frontier.removeChanges();
        } else if (personal && minecraft.player != null && frontier.getOwner().equals(new SettingsUser(minecraft.player))) {
            FrontierOverlay frontierOverlay = updateFrontier(frontier);
            if (frontierOverlay != null) {
                ClientEventHandler.postUpdatedFrontierEvent(frontierOverlay, minecraft.player.getId());
            }
        }
    }

    public void clientShareFrontier(UUID frontierID, SettingsUser targetUser) {
        if (MapFrontiersClient.isModOnServer()) {
            SettingsUserShared sharedUser = new SettingsUserShared(targetUser, false);
            sharedUser.setActions(EnumSet.noneOf(SettingsUserShared.Action.class));
            PacketHandler.sendToServer(new PacketSharePersonalFrontier(frontierID, sharedUser));
        }
    }

    public FrontierOverlay deleteFrontier(UUID id) {
        for (ResourceKey<Level> dimension : dimensionsFrontiers.keySet()) {
            FrontierOverlay frontierOverlay = deleteFrontier(dimension, id);
            if (frontierOverlay != null) {
                return frontierOverlay;
            }
        }

        return null;
    }

    public FrontierOverlay deleteFrontier(ResourceKey<Level> dimension, UUID id) {
        List<FrontierOverlay> frontiers = getAllFrontiers(dimension);

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(id));

        if (index < 0) {
            return null;
        }

        FrontierOverlay frontier = frontiers.remove(index);
        frontier.deleted();

        if (personal && !minecraft.isLocalServer()) {
            saveData();
        }

        return frontier;
    }

    public FrontierOverlay updateFrontier(FrontierData data) {
        List<FrontierOverlay> frontiers = getAllFrontiers(data.getDimension());

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(data.getId()));

        if (index < 0) {
            return null;
        } else {
            FrontierOverlay frontierOverlay = frontiers.get(index);
            frontierOverlay.updateFromData(data);
            frontierOverlay.setModified(new Date());

            if (personal && !minecraft.isLocalServer()) {
                saveData();
            }

            return frontierOverlay;
        }
    }

    public Map<ResourceKey<Level>, ArrayList<FrontierOverlay>> getAllFrontiers() {
        ensureLoadData();
        return dimensionsFrontiers;
    }

    public List<FrontierOverlay> getAllFrontiers(ResourceKey<Level> dimension) {
        ensureLoadData();
        return dimensionsFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public List<FrontierOverlay> getFrontiersInPosition(ResourceKey<Level> dimension, BlockPos pos, double maxDistanceToOpen) {
        List<FrontierOverlay> frontiersInPosition = new ArrayList<>();
        ArrayList<FrontierOverlay> frontiers = dimensionsFrontiers.get(dimension);
        if (frontiers != null) {
            for (FrontierOverlay frontier : frontiers) {
                if (frontier.getVisibility(FrontierData.VisibilityData.Visibility.Frontier) && frontier.pointIsInside(pos, maxDistanceToOpen)) {
                    frontiersInPosition.add(frontier);
                }
            }
        }

        return frontiersInPosition;
    }

    public Set<FrontierOverlay> getFrontiersForAnnounce(ResourceKey<Level> dimension, BlockPos pos) {
        Set<FrontierOverlay> inPosition = new HashSet<>();
        if (Config.announceInChat == Config.Visibility.Never && Config.announceInTitle == Config.Visibility.Never) {
            return inPosition;
        }

        ArrayList<FrontierOverlay> frontiers = dimensionsFrontiers.get(dimension);
        boolean forcedAnnounceInChat = Config.announceInChat == Config.Visibility.Always;
        boolean forcedAnnounceInTitle = Config.announceInTitle == Config.Visibility.Always;
        if (frontiers != null) {
            for (FrontierOverlay frontier : frontiers) {
                if ((forcedAnnounceInChat || forcedAnnounceInTitle || frontier.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInChat) || frontier.getVisibility(FrontierData.VisibilityData.Visibility.AnnounceInTitle)) && frontier.pointIsInside(pos, 0.0)) {
                    inPosition.add(frontier);
                }
            }
        }

        return inPosition;
    }

    @Nullable
    public FrontierOverlay getFrontierCopiedFrom(UUID copiedFromId) {

        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                if (frontier.wasCopied() && frontier.getCopiedFromId().equals(copiedFromId)) {
                    return frontier;
                }
            }
        }
        return null;
    }

    @Nullable
    public FrontierOverlay getFrontier(UUID frontierId) {
        for (List<FrontierOverlay> frontiers : dimensionsFrontiers.values()) {
            for (FrontierOverlay frontier : frontiers) {
                if (frontier.getId().equals(frontierId)) {
                    return frontier;
                }
            }
        }

        return null;
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

    public void updateSelectedMarker(ResourceKey<Level> dimension, @Nullable FrontierOverlay frontier) {
        MarkerOverlay marker = markersSelected.get(dimension);
        if (marker != null) {
            jmAPI.remove(marker);
        }

        if (frontier != null) {
            BlockPos pos = frontier.getSelectedVertex();
            if (pos != null) {
                marker = new MarkerOverlay(MapFrontiers.MODID, pos, markerDotSelected);
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

    private boolean readFromNBT(CompoundTag nbt) {
        boolean needBackup = false;
        try {
            int version = nbt.getInt("Version");
            if (version == 0) {
                MapFrontiers.LOGGER.warn("Data version in personal_frontiers not found, expected " + MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            } else if (version > MapFrontiers.FRONTIER_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in personal_frontiers higher than expected. The mod uses " + MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            }

            ListTag frontiersTagList = nbt.getList("frontiers", Tag.TAG_COMPOUND);
            for (int i = 0; i < frontiersTagList.size(); ++i) {
                FrontierData frontier = new FrontierData();
                CompoundTag frontierTag = frontiersTagList.getCompound(i);
                frontier.readFromNBT(frontierTag, version);
                List<FrontierOverlay> frontiers = getAllFrontiers(frontier.getDimension());
                FrontierOverlay frontierOverlay = new FrontierOverlay(frontier, jmAPI);
                frontiers.add(frontierOverlay);
            }
        } catch (Exception ignored) {
            return true;
        }

        return needBackup;
    }

    private void writeToNBT(CompoundTag nbt) {
        ListTag frontiersTagList = new ListTag();
        if (minecraft.player != null) {
            SettingsUser playerUser = new SettingsUser(minecraft.player);
            for (Map.Entry<ResourceKey<Level>, ArrayList<FrontierOverlay>> frontiers : dimensionsFrontiers.entrySet()) {
                for (FrontierData frontier : frontiers.getValue()) {
                    if (frontier.getPersonal() && frontier.getOwner().equals(playerUser)) {
                        CompoundTag frontierTag = new CompoundTag();
                        frontier.writeToNBT(frontierTag);
                        frontiersTagList.add(frontierTag);
                    }
                }
            }
        }
        nbt.put("frontiers", frontiersTagList);

        nbt.putInt("Version", MapFrontiers.FRONTIER_DATA_VERSION);
    }

    private void ensureLoadData() {
        if (personal && !minecraft.isLocalServer() && ModDir == null) {
            loadData();
        }
    }

    private void loadData() {
        try {
            File jmDir = Services.JOURNEYMAP.getJMWorldDir(Minecraft.getInstance());
            ModDir = new File(jmDir, "mapfrontier");
            //noinspection ResultOfMethodCallIgnored
            ModDir.mkdirs();

            CompoundTag nbtFrontiers = loadFile("personal_frontiers.dat");
            if (!nbtFrontiers.isEmpty()) {
                if (readFromNBT(nbtFrontiers)) {
                    MapFrontiers.createBackup(ModDir, "personal_frontiers.dat");
                    saveData();
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    private void saveData() {
        if (personal && !minecraft.isLocalServer() && ModDir != null) {
            CompoundTag nbtFrontiers = new CompoundTag();
            writeToNBT(nbtFrontiers);
            saveFile("personal_frontiers.dat", nbtFrontiers);
        }
    }

    private CompoundTag loadFile(String filename) {
        File f = new File(ModDir, filename);
        if (f.exists()) {
            try (FileInputStream inputStream = new FileInputStream(f)) {
                return NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        }

        return new CompoundTag();
    }

    private void saveFile(String filename, CompoundTag nbt) {
        try {
            File f = new File(ModDir, filename);
            try (FileOutputStream outputStream = new FileOutputStream(f)) {
                NbtIo.writeCompressed(nbt, outputStream);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }
}
