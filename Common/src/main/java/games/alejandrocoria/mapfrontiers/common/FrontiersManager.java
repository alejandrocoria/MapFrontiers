package games.alejandrocoria.mapfrontiers.common;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.event.EventHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierUpdated;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.ColorHelper;
import games.alejandrocoria.mapfrontiers.common.util.ContainerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class FrontiersManager {
    public static FrontiersManager instance;

    private final HashMap<UUID, FrontierData> allFrontiers;
    private final HashMap<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsGlobalFrontiers;
    private final HashMap<SettingsUser, HashMap<ResourceKey<Level>, ArrayList<FrontierData>>> usersDimensionsPersonalFrontiers;
    private final HashMap<Integer, PendingShareFrontier> pendingShareFrontiers;
    private int pendingShareFrontiersTick = 0;
    private FrontierSettings frontierSettings;
    private File ModDir;
    private boolean frontierOwnersChecked = false;

    private static int pendingShareFrontierID = 0;
    private static final int pendingShareFrontierTickDuration = 1200;

    public FrontiersManager() {
        instance = this;
        allFrontiers = new HashMap<>();
        dimensionsGlobalFrontiers = new HashMap<>();
        usersDimensionsPersonalFrontiers = new HashMap<>();
        pendingShareFrontiers = new HashMap<>();
        frontierSettings = new FrontierSettings();

        EventHandler.subscribeServerTickEvent(this, server -> {
            ++pendingShareFrontiersTick;

            if (pendingShareFrontiersTick >= 100) {
                pendingShareFrontiersTick -= 100;

                for (PendingShareFrontier pending : pendingShareFrontiers.values()) {
                    pending.tickCount += 100;

                    if (pending.tickCount >= pendingShareFrontierTickDuration) {
                        FrontierData frontier = getFrontierFromID(pending.frontierID);
                        if (frontier == null) {
                            continue;
                        }

                        if (frontier.getUsersShared() != null) {
                            boolean removed = frontier.getUsersShared().removeIf(x -> x.getUser().equals(pending.targetUser));
                            if (removed) {
                                PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier), frontier, server);
                            }
                        }
                    }
                }

                pendingShareFrontiers.entrySet().removeIf(x -> x.getValue().tickCount >= pendingShareFrontierTickDuration);
            }
        });
    }

    public void close() {
        EventHandler.unsubscribeAllEvents(this);
    }

    public void setSettings(FrontierSettings frontierSettings) {
        this.frontierSettings = frontierSettings;
        saveSettingsData();
    }

    public FrontierSettings getSettings() {
        return frontierSettings;
    }

    public Map<ResourceKey<Level>, ArrayList<FrontierData>> getAllGlobalFrontiers() {
        return dimensionsGlobalFrontiers;
    }

    public List<FrontierData> getAllGlobalFrontiers(ResourceKey<Level> dimension) {
        return dimensionsGlobalFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public Map<ResourceKey<Level>, ArrayList<FrontierData>> getAllPersonalFrontiers(SettingsUser user) {
        return usersDimensionsPersonalFrontiers.computeIfAbsent(user, k -> new HashMap<>());
    }

    public List<FrontierData> getAllPersonalFrontiers(SettingsUser user, ResourceKey<Level> dimension) {
        HashMap<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers
                .computeIfAbsent(user, k -> new HashMap<>());

        return dimensionsPersonalFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public FrontierData getFrontierFromID(UUID id) {
        return allFrontiers.get(id);
    }

    public FrontierData createNewGlobalFrontier(UUID frontierId, ResourceKey<Level> dimension, ServerPlayer player, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        List<FrontierData> frontiers = getAllGlobalFrontiers(dimension);
        return createNewFrontier(frontierId, frontiers, dimension, false, player, vertices, chunks);
    }

    public FrontierData createNewPersonalFrontier(UUID frontierId, ResourceKey<Level> dimension, ServerPlayer player, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        List<FrontierData> frontiers = getAllPersonalFrontiers(new SettingsUser(player), dimension);
        return createNewFrontier(frontierId, frontiers, dimension, true, player, vertices, chunks);
    }

    private FrontierData createNewFrontier(UUID frontierId, List<FrontierData> frontiers, ResourceKey<Level> dimension, boolean personal, ServerPlayer player, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        FrontierData frontier = new FrontierData();
        frontier.setId(frontierId);
        frontier.setOwner(new SettingsUser(player));
        frontier.setDimension(dimension);
        frontier.setPersonal(personal);
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

        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);
        saveFrontierData();

        return frontier;
    }

    public void addPersonalFrontier(FrontierData frontier) {
        if (!frontier.getPersonal()) {
            return;
        }

        List<FrontierData> frontiers = getAllPersonalFrontiers(frontier.getOwner(), frontier.getDimension());
        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);

        saveFrontierData();
    }

    public void addGlobalFrontier(FrontierData frontier) {
        if (frontier.getPersonal()) {
            return;
        }

        List<FrontierData> frontiers = getAllGlobalFrontiers(frontier.getDimension());
        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);

        saveFrontierData();
    }

    public void addPersonalFrontier(SettingsUser user, FrontierData frontier) {
        List<FrontierData> frontiers = this.getAllPersonalFrontiers(user, frontier.getDimension());
        frontiers.add(frontier);
        saveFrontierData();
    }

    public boolean deleteGlobalFrontier(ResourceKey<Level> dimension, UUID id) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(dimension);
        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.id.equals(id));
        deleted |= allFrontiers.remove(id) != null;

        if (deleted) {
            saveFrontierData();
        }

        return deleted;
    }

    public boolean deletePersonalFrontier(SettingsUser user, ResourceKey<Level> dimension, UUID id) {
        Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(dimension);
        if (frontiers == null) {
            return false;
        }

        if (allFrontiers.get(id).getOwner().equals(user)) {
            allFrontiers.remove(id);
        }

        boolean deleted = frontiers.removeIf(x -> x.id.equals(id));

        if (deleted) {
            saveFrontierData();
        }

        return deleted;
    }

    public boolean updateGlobalFrontier(FrontierData updatedFrontier) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(updatedFrontier.getDimension());
        if (frontiers == null) {
            return false;
        }

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(updatedFrontier.getId()));

        if (index < 0) {
            return false;
        }

        updatedFrontier.setModified(new Date());

        FrontierData frontier = frontiers.get(index);
        frontier.updateFromData(updatedFrontier);

        saveFrontierData();

        return true;
    }

    public boolean updatePersonalFrontier(SettingsUser user, FrontierData updatedFrontier) {
        Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(updatedFrontier.getDimension());
        if (frontiers == null) {
            return false;
        }

        int index = ContainerHelper.getIndexFromLambda(frontiers, i -> frontiers.get(i).getId().equals(updatedFrontier.getId()));

        if (index < 0) {
            return false;
        }

        updatedFrontier.setModified(new Date());

        FrontierData frontier = frontiers.get(index);
        frontier.updateFromData(updatedFrontier);

        saveFrontierData();

        return true;
    }

    public boolean changePersonalFrontierToGlobal(SettingsUser user, ResourceKey<Level> dimension, UUID id) {
        Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(dimension);
        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.id.equals(id));
        if (deleted) {
            FrontierData frontier = allFrontiers.get(id);
            if (frontier.getOwner().equals(user)) {
                if (frontier.getUsersShared() != null) {
                    for (SettingsUserShared userShared : frontier.getUsersShared()) {
                        changePersonalFrontierToGlobal(userShared.getUser(), dimension, id);
                    }
                }
                frontier.setPersonal(false);
                frontier.setModified(new Date());
                frontier.removeAllUserShared();
                frontier.removeChanges();
                getAllGlobalFrontiers(dimension).add(frontier);
                saveFrontierData();
            }
        }

        return deleted;
    }

    public boolean changeGlobalFrontierToPersonal(SettingsUser newOwner, ResourceKey<Level> dimension, UUID id) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(dimension);

        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.id.equals(id));
        if (deleted) {
            FrontierData frontier = allFrontiers.get(id);
            frontier.setPersonal(true);
            frontier.setModified(new Date());
            frontier.setOwner(newOwner);
            getAllPersonalFrontiers(newOwner, dimension).add(frontier);
            saveFrontierData();
        }

        return deleted;
    }

    public boolean hasPersonalFrontier(SettingsUser user, UUID frontierID) {
        for (List<FrontierData> frontiers : getAllPersonalFrontiers(user).values()) {
            for (FrontierData frontier : frontiers) {
                if (frontier.getId().equals(frontierID)) {
                    return true;
                }
            }
        }

        return false;
    }

    public int addShareMessage(SettingsUser targetUser, UUID frontierID) {
        ++pendingShareFrontierID;
        if (pendingShareFrontierID == 1000) {
            pendingShareFrontierID = 1;
        }
        pendingShareFrontiers.put(pendingShareFrontierID, new PendingShareFrontier(frontierID, targetUser));

        return pendingShareFrontierID;
    }

    public PendingShareFrontier getPendingShareFrontier(int messageID) {
        return pendingShareFrontiers.get(messageID);
    }

    public void removePendingShareFrontier(int messageID) {
        pendingShareFrontiers.remove(messageID);
    }

    public void removePendingShareFrontier(SettingsUser user) {
        pendingShareFrontiers.entrySet().removeIf(x -> x.getValue().targetUser.equals(user));
    }

    public boolean canSendCommandAcceptFrontier(ServerPlayer player) {
        return frontierSettings.checkAction(FrontierSettings.Action.SharePersonalFrontier, new SettingsUser(player),
                MapFrontiers.isOPorHost(player), null);
    }

    public void ensureOwners(MinecraftServer server) {
        if (frontierOwnersChecked) {
            return;
        }

        for (FrontierData frontier : allFrontiers.values()) {
            frontier.ensureOwner(server);
        }

        frontierOwnersChecked = true;
    }

    private boolean readFromNBT(CompoundTag nbt) {
        boolean needBackup = false;
        try {
            int version = nbt.getIntOr("Version", 0);
            if (version == 0) {
                MapFrontiers.LOGGER.warn("Data version in frontiers not found, expected " + MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            } else if (version < 5) {
                MapFrontiers.LOGGER.warn("Data version in frontiers lower than expected. The mod support from 5 to " + MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            } else if (version > MapFrontiers.FRONTIER_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in frontiers higher than expected. The mod uses " + MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            }

            ListTag allFrontiersTagList = nbt.getListOrEmpty("frontiers");
            for (int i = 0; i < allFrontiersTagList.size(); ++i) {
                FrontierData frontier = new FrontierData();
                CompoundTag frontierTag = allFrontiersTagList.getCompound(i).get();
                frontier.readFromNBT(frontierTag, version);
                frontier.removePendingUsersShared();
                allFrontiers.put(frontier.getId(), frontier);

                if (frontier.getPersonal()) {
                    getAllPersonalFrontiers(frontier.getOwner(), frontier.getDimension()).add(frontier);

                    if (frontier.getUsersShared() != null) {
                        for (SettingsUserShared sharedUser : frontier.getUsersShared()) {
                            getAllPersonalFrontiers(sharedUser.getUser(), frontier.getDimension()).add(frontier);
                        }
                    }
                } else {
                    getAllGlobalFrontiers(frontier.getDimension()).add(frontier);
                }
            }
        } catch (Exception ignored) {
            return true;
        }

        return needBackup;
    }

    private void writeToNBT(CompoundTag nbt) {
        ListTag allFrontiersTagList = new ListTag();
        for (FrontierData frontier : allFrontiers.values()) {
            CompoundTag frontierTag = new CompoundTag();
            frontier.writeToNBT(frontierTag);
            allFrontiersTagList.add(frontierTag);
        }
        nbt.put("frontiers", allFrontiersTagList);

        nbt.putInt("Version", MapFrontiers.FRONTIER_DATA_VERSION);
    }

    public void loadOrCreateData(MinecraftServer server) {
        try {
            File mcDir;
            if (server.isDedicatedServer()) {
                mcDir = server.getServerDirectory().toFile();
            } else if (Minecraft.getInstance().getSingleplayerServer() != null) {
                mcDir = Minecraft.getInstance().getSingleplayerServer().getWorldPath(LevelResource.ROOT).toFile();
            } else {
                mcDir = Minecraft.getInstance().gameDirectory;
            }
            if (mcDir.getPath().isEmpty()) {
                mcDir = new File(".");
            }
            ModDir = new File(mcDir, "mapfrontiers");
            if (ModDir.mkdirs()) {
                MapFrontiers.LOGGER.info("Created folder: " + ModDir.toString());
            }

            CompoundTag nbtFrontiers = loadFile("frontiers.dat");
            if (nbtFrontiers.isEmpty()) {
                writeToNBT(nbtFrontiers);
                saveFile("frontiers.dat", nbtFrontiers);
            } else {
                if (readFromNBT(nbtFrontiers)) {
                    MapFrontiers.createBackup(ModDir, "frontiers.dat");
                    saveFrontierData();
                }
            }

            CompoundTag nbtSettings = loadFile("settings.dat");
            if (nbtSettings.isEmpty()) {
                frontierSettings.resetToDefault();
                frontierSettings.writeToNBT(nbtSettings);
                saveFile("settings.dat", nbtSettings);
            } else {
                if (frontierSettings.readFromNBT(nbtSettings)) {
                    MapFrontiers.createBackup(ModDir, "settings.dat");
                    saveSettingsData();
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    private void saveFrontierData() {
        CompoundTag nbtFrontiers = new CompoundTag();
        writeToNBT(nbtFrontiers);
        saveFile("frontiers.dat", nbtFrontiers);
    }

    private void saveSettingsData() {
        CompoundTag nbtSettings = new CompoundTag();
        frontierSettings.writeToNBT(nbtSettings);
        saveFile("settings.dat", nbtSettings);
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
