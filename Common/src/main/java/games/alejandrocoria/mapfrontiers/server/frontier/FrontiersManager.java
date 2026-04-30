package games.alejandrocoria.mapfrontiers.server.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierCreationFactory;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtFileHelper;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class FrontiersManager {
    private static final long FRONTIERS_UPDATE_SAVE_DEBOUNCE_MS = 10_000L;
    private static final long FRONTIERS_UPDATE_SAVE_MAX_DELAY_MS = 60_000L;

    private final HashMap<UUID, FrontierData> allFrontiers;
    private final HashMap<UUID, CollectionData> allCollections;
    private final HashMap<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsGlobalFrontiers;
    private final ArrayList<CollectionData> globalCollections;
    private final HashMap<SettingsUser, HashMap<ResourceKey<Level>, ArrayList<FrontierData>>> usersDimensionsPersonalFrontiers;
    private final HashMap<SettingsUser, ArrayList<CollectionData>> usersPersonalCollections;
    private final HashMap<Integer, PendingShareFrontier> pendingShareFrontiers;
    private FrontierSettings frontierSettings;
    private File ModDir;
    private boolean frontierOwnersChecked = false;
    private boolean frontiersDirty = false;
    private long lastFrontiersUpdateAt = 0L;
    private long lastFrontiersSaveAt = 0L;

    private static int pendingShareFrontierID = 0;

    public FrontiersManager() {
        allFrontiers = new HashMap<>();
        allCollections = new HashMap<>();
        dimensionsGlobalFrontiers = new HashMap<>();
        globalCollections = new ArrayList<>();
        usersDimensionsPersonalFrontiers = new HashMap<>();
        usersPersonalCollections = new HashMap<>();
        pendingShareFrontiers = new HashMap<>();
        frontierSettings = new FrontierSettings();
    }

    public void close() {
        if (frontiersDirty) {
            saveFrontiersNow();
        }
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

    public @Nullable CollectionData getCollectionFromID(UUID id) {
        return allCollections.get(id);
    }

    public List<CollectionData> getAllGlobalCollections() {
        return globalCollections;
    }

    public List<CollectionData> getAllPersonalCollections(SettingsUser user) {
        return usersPersonalCollections.computeIfAbsent(user, k -> new ArrayList<>());
    }

    public List<FrontierData> getFrontiersInCollection(UUID collectionId) {
        ArrayList<FrontierData> frontiers = new ArrayList<>();
        for (FrontierData frontier : allFrontiers.values()) {
            if (collectionId.equals(frontier.getCollectionId())) {
                frontiers.add(frontier);
            }
        }

        return frontiers;
    }

    public boolean userHasVisiblePersonalCollection(SettingsUser user, UUID collectionId) {
        for (ArrayList<FrontierData> frontiers : getAllPersonalFrontiers(user).values()) {
            for (FrontierData frontier : frontiers) {
                if (collectionId.equals(frontier.getCollectionId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public FrontierData createNewGlobalFrontier(UUID frontierId,
                                                ResourceKey<Level> dimension,
                                                ServerPlayer player,
                                                @Nullable String sourcePluginId,
                                                @Nullable List<BlockPos> vertices,
                                                @Nullable List<ChunkPos> chunks,
                                                @Nullable List<BlockPos> points,
                                                @Nullable FrontierData.PathStyle pathStyle) {
        List<FrontierData> frontiers = getAllGlobalFrontiers(dimension);
        return createNewFrontier(frontierId, frontiers, dimension, false, player, sourcePluginId, vertices, chunks, points, pathStyle);
    }

    public FrontierData createNewPersonalFrontier(UUID frontierId,
                                                  ResourceKey<Level> dimension,
                                                  ServerPlayer player,
                                                  @Nullable String sourcePluginId,
                                                  @Nullable List<BlockPos> vertices,
                                                  @Nullable List<ChunkPos> chunks,
                                                  @Nullable List<BlockPos> points,
                                                  @Nullable FrontierData.PathStyle pathStyle) {
        List<FrontierData> frontiers = getAllPersonalFrontiers(new SettingsUser(player), dimension);
        return createNewFrontier(frontierId, frontiers, dimension, true, player, sourcePluginId, vertices, chunks, points, pathStyle);
    }

    private FrontierData createNewFrontier(UUID frontierId,
                                           List<FrontierData> frontiers,
                                           ResourceKey<Level> dimension,
                                           boolean personal,
                                           ServerPlayer player,
                                           @Nullable String sourcePluginId,
                                           @Nullable List<BlockPos> vertices,
                                           @Nullable List<ChunkPos> chunks,
                                           @Nullable List<BlockPos> points,
                                           @Nullable FrontierData.PathStyle pathStyle) {
        FrontierData frontier = FrontierCreationFactory.createFrontier(frontierId, new SettingsUser(player), dimension, personal,
                FrontierData.FrontierLifetime.PERSISTENT, sourcePluginId, vertices, chunks, points, pathStyle);

        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);
        saveFrontiersNow();

        return frontier;
    }

    public void addPersonalFrontier(FrontierData frontier) {
        if (!frontier.getPersonal()) {
            return;
        }

        List<FrontierData> frontiers = getAllPersonalFrontiers(frontier.getOwner(), frontier.getDimension());
        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);
        saveFrontiersNow();
    }

    public void importPersonalFrontier(FrontierData frontier) {
        if (!frontier.getPersonal()) {
            return;
        }

        List<FrontierData> frontiers = getAllPersonalFrontiers(frontier.getOwner(), frontier.getDimension());
        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);
        markFrontiersUpdated();
    }

    public void addGlobalFrontier(FrontierData frontier) {
        if (frontier.getPersonal()) {
            return;
        }

        List<FrontierData> frontiers = getAllGlobalFrontiers(frontier.getDimension());
        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);
        saveFrontiersNow();
    }

    public void addGlobalCollection(CollectionData collection) {
        if (collection.getPersonal()) {
            return;
        }

        globalCollections.add(collection);
        allCollections.put(collection.getId(), collection);
        saveFrontiersNow();
    }

    public void addPersonalCollection(CollectionData collection) {
        if (!collection.getPersonal()) {
            return;
        }

        getAllPersonalCollections(collection.getOwner()).add(collection);
        allCollections.put(collection.getId(), collection);
        saveFrontiersNow();
    }

    public void importPersonalCollection(CollectionData collection) {
        if (!collection.getPersonal()) {
            return;
        }

        List<CollectionData> collections = getAllPersonalCollections(collection.getOwner());
        collections.add(collection);
        allCollections.put(collection.getId(), collection);
        markFrontiersUpdated();
    }

    public void addPersonalFrontier(SettingsUser user, FrontierData frontier) {
        List<FrontierData> frontiers = this.getAllPersonalFrontiers(user, frontier.getDimension());
        frontiers.add(frontier);
        saveFrontiersNow();
    }

    public boolean deleteGlobalFrontier(ResourceKey<Level> dimension, UUID id) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(dimension);
        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.getId().equals(id));
        deleted |= allFrontiers.remove(id) != null;

        if (deleted) {
            saveFrontiersNow();
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

        boolean deleted = frontiers.removeIf(x -> x.getId().equals(id));

        if (deleted) {
            saveFrontiersNow();
        }

        return deleted;
    }

    public boolean applyGlobalFrontierChange(UUID frontierId, FrontierChange change) {
        FrontierData frontier = allFrontiers.get(frontierId);
        if (frontier == null || frontier.getPersonal()) {
            return false;
        }

        frontier.setModified(new Date());
        change.setModifiedTime(frontier.getModified().getTime());
        frontier.applyChange(change);
        markFrontiersUpdated();
        return true;
    }

    public boolean applyPersonalFrontierChange(SettingsUser user, UUID frontierId, FrontierChange change) {
        Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        FrontierData frontier = allFrontiers.get(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(frontier.getDimension());
        if (frontiers == null || frontiers.stream().noneMatch(existing -> existing.getId().equals(frontierId))) {
            return false;
        }

        frontier.setModified(new Date());
        change.setModifiedTime(frontier.getModified().getTime());
        frontier.applyChange(change);
        markFrontiersUpdated();
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

        boolean deleted = frontiers.removeIf(x -> x.getId().equals(id));
        if (deleted) {
            FrontierData frontier = allFrontiers.get(id);
            if (frontier.getOwner().equals(user)) {
                if (frontier.getUsersShared() != null) {
                    for (SettingsUserShared userShared : frontier.getUsersShared()) {
                        changePersonalFrontierToGlobal(userShared.getUser(), dimension, id);
                    }
                }
                frontier.setPersonal(false);
                frontier.setCollectionId(null);
                frontier.setModified(new Date());
                frontier.removeAllUserShared();
                getAllGlobalFrontiers(dimension).add(frontier);
                saveFrontiersNow();
            }
        }

        return deleted;
    }

    public boolean changeGlobalFrontierToPersonal(SettingsUser newOwner, ResourceKey<Level> dimension, UUID id) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(dimension);

        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.getId().equals(id));
        if (deleted) {
            FrontierData frontier = allFrontiers.get(id);
            frontier.setPersonal(true);
            frontier.setCollectionId(null);
            frontier.setModified(new Date());
            frontier.setOwner(newOwner);
            getAllPersonalFrontiers(newOwner, dimension).add(frontier);
            saveFrontiersNow();
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

    public Map<Integer, PendingShareFrontier> getPendingShareFrontiers() {
        return pendingShareFrontiers;
    }

    public void removePendingShareFrontier(int messageID) {
        pendingShareFrontiers.remove(messageID);
    }

    public void removePendingShareFrontier(SettingsUser user) {
        pendingShareFrontiers.entrySet().removeIf(x -> x.getValue().targetUser.equals(user));
    }

    public void ensureOwners(MinecraftServer server) {
        if (frontierOwnersChecked) {
            return;
        }

        for (FrontierData frontier : allFrontiers.values()) {
            frontier.ensureOwner(server);
        }

        for (CollectionData collection : allCollections.values()) {
            if (collection.getOwner().isEmpty()) {
                if (server.isDedicatedServer()) {
                    continue;
                }

                List<ServerPlayer> playerList = server.getPlayerList().getPlayers();
                if (!playerList.isEmpty()) {
                    collection.setOwner(new SettingsUser(playerList.getFirst()));
                }
            } else {
                collection.getOwner().fillMissingInfo(false, server);
            }
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

            ListTag allCollectionsTagList = nbt.getListOrEmpty("collections");
            for (int i = 0; i < allCollectionsTagList.size(); ++i) {
                try {
                    CollectionData collection = new CollectionData();
                    CompoundTag collectionTag = NbtReadHelper.requireCompound(allCollectionsTagList, i, "collections");
                    collection.readFromNBT(collectionTag, version);
                    allCollections.put(collection.getId(), collection);

                    if (collection.getPersonal()) {
                        getAllPersonalCollections(collection.getOwner()).add(collection);
                    } else {
                        getAllGlobalCollections().add(collection);
                    }
                } catch (InvalidNbtFormatException e) {
                    MapFrontiers.LOGGER.warn("Skipping invalid collection at collections[{}]: {}", i, e.getMessage());
                    needBackup = true;
                }
            }

            ListTag allFrontiersTagList = nbt.getListOrEmpty("frontiers");
            for (int i = 0; i < allFrontiersTagList.size(); ++i) {
                try {
                    FrontierData frontier = new FrontierData();
                    CompoundTag frontierTag = NbtReadHelper.requireCompound(allFrontiersTagList, i, "frontiers");
                    frontier.readFromNBT(frontierTag, version);
                    frontier.removePendingUsersShared();
                    allFrontiers.put(frontier.getId(), frontier);

                    if (frontier.hasCollection()) {
                        CollectionData collection = allCollections.get(frontier.getCollectionId());
                        boolean invalidCollectionReference = collection == null
                                || collection.getPersonal() != frontier.getPersonal()
                                || (frontier.getPersonal() && !collection.getOwner().equals(frontier.getOwner()));
                        if (invalidCollectionReference) {
                            frontier.setCollectionId(null);
                            needBackup = true;
                        }
                    }

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
                } catch (InvalidNbtFormatException e) {
                    MapFrontiers.LOGGER.warn("Skipping invalid frontier at frontiers[{}]: {}", i, e.getMessage());
                    needBackup = true;
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.warn("Failed to read frontiers.dat: {}", e.getMessage());
            return true;
        }

        return needBackup;
    }

    private void writeToNBT(CompoundTag nbt) {
        ListTag allCollectionsTagList = new ListTag();
        for (CollectionData collection : allCollections.values()) {
            CompoundTag collectionTag = new CompoundTag();
            collection.writeToNBT(collectionTag);
            allCollectionsTagList.add(collectionTag);
        }
        nbt.put("collections", allCollectionsTagList);

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
            File mcDir = NbtFileHelper.resolveServerRootDir(server);
            ModDir = new File(mcDir, "mapfrontiers");
            if (ModDir.mkdirs()) {
                MapFrontiers.LOGGER.info("Created MapFrontiers data directory at {}", ModDir);
            }

            CompoundTag nbtFrontiers = loadFile("frontiers.dat");
            if (nbtFrontiers.isEmpty()) {
                writeToNBT(nbtFrontiers);
                saveFile("frontiers.dat", nbtFrontiers);
                lastFrontiersSaveAt = System.currentTimeMillis();
            } else {
                if (readFromNBT(nbtFrontiers)) {
                    NbtFileHelper.createBackup(ModDir, "frontiers.dat");
                    saveFrontiersNow();
                } else {
                    lastFrontiersSaveAt = System.currentTimeMillis();
                }
            }

            CompoundTag nbtSettings = loadFile("settings.dat");
            if (nbtSettings.isEmpty()) {
                frontierSettings.resetToDefault();
                frontierSettings.writeToNBT(nbtSettings);
                saveFile("settings.dat", nbtSettings);
            } else {
                if (frontierSettings.readFromNBT(nbtSettings)) {
                    NbtFileHelper.createBackup(ModDir, "settings.dat");
                    saveSettingsData();
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    public void markFrontiersUpdated() {
        frontiersDirty = true;
        lastFrontiersUpdateAt = System.currentTimeMillis();
    }

    public void flushPendingFrontierUpdates() {
        if (!frontiersDirty) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean debounceElapsed = now - lastFrontiersUpdateAt >= FRONTIERS_UPDATE_SAVE_DEBOUNCE_MS;
        boolean maxDelayElapsed = lastFrontiersSaveAt == 0L
                || now - lastFrontiersSaveAt >= FRONTIERS_UPDATE_SAVE_MAX_DELAY_MS;

        if (debounceElapsed || maxDelayElapsed) {
            saveFrontiersNow();
        }
    }

    public void saveFrontiersNow() {
        CompoundTag nbtFrontiers = new CompoundTag();
        writeToNBT(nbtFrontiers);
        saveFile("frontiers.dat", nbtFrontiers);
        frontiersDirty = false;
        lastFrontiersSaveAt = System.currentTimeMillis();
    }

    public @Nullable CollectionData removeCollection(UUID collectionId) {
        CollectionData collection = allCollections.remove(collectionId);
        if (collection == null) {
            return null;
        }

        if (collection.getPersonal()) {
            getAllPersonalCollections(collection.getOwner()).removeIf(existing -> existing.getId().equals(collectionId));
        } else {
            globalCollections.removeIf(existing -> existing.getId().equals(collectionId));
        }

        saveFrontiersNow();
        return collection;
    }

    public boolean deleteCollection(UUID collectionId) {
        CollectionData collection = removeCollection(collectionId);
        if (collection == null) {
            return false;
        }

        for (FrontierData frontier : allFrontiers.values()) {
            if (collectionId.equals(frontier.getCollectionId())) {
                frontier.setCollectionId(null);
            }
        }

        saveFrontiersNow();
        return true;
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
        NbtFileHelper.saveCompressedNbtSafely(ModDir, filename, nbt);
    }
}
