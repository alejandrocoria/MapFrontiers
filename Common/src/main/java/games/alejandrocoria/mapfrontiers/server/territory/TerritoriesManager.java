package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerIdLookup;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerReferenceCollector;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtReadContext;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChangeApplicationResult;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreateSpec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierCreationFactory;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import games.alejandrocoria.mapfrontiers.common.util.DebouncedPersistenceController;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtFileHelper;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class TerritoriesManager {
    private static final long TERRITORIES_UPDATE_SAVE_DEBOUNCE_MS = 10_000L;
    private static final long TERRITORIES_UPDATE_SAVE_MAX_DELAY_MS = 60_000L;

    private final HashMap<UUID, FrontierData> allFrontiers;
    private final HashMap<UUID, CollectionData> allCollections;
    private final HashMap<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsGlobalFrontiers;
    private final ArrayList<CollectionData> globalCollections;
    private final HashMap<PlayerId, HashMap<ResourceKey<Level>, ArrayList<FrontierData>>> usersDimensionsPersonalFrontiers;
    private final HashMap<PlayerId, ArrayList<CollectionData>> usersPersonalCollections;
    private final HashMap<UUID, LinkedHashSet<UUID>> frontierIdsByCollectionId;
    private final HashMap<ResourceKey<Level>, LinkedHashSet<UUID>> globalFrontierIdsByDimension;
    private final HashMap<PlayerId, HashMap<ResourceKey<Level>, LinkedHashSet<UUID>>> knownPersonalFrontierIdsByUserAndDimension;
    private final DebouncedPersistenceController persistenceController;
    private final PlayerNameRepository playerNames;
    private final PlayerReferenceNbtReadContext playerReferenceReadContext;
    private FrontierSettings frontierSettings;
    private long settingsRevision;
    private File ModDir;

    public TerritoriesManager(PlayerNameRepository playerNames, PlayerIdLookup nameOnlyLookup) {
        this.playerNames = Objects.requireNonNull(playerNames, "playerNames");
        playerReferenceReadContext = new PlayerReferenceNbtReadContext(playerNames,
                Objects.requireNonNull(nameOnlyLookup, "nameOnlyLookup"));
        allFrontiers = new HashMap<>();
        allCollections = new HashMap<>();
        dimensionsGlobalFrontiers = new HashMap<>();
        globalCollections = new ArrayList<>();
        usersDimensionsPersonalFrontiers = new HashMap<>();
        usersPersonalCollections = new HashMap<>();
        frontierIdsByCollectionId = new HashMap<>();
        globalFrontierIdsByDimension = new HashMap<>();
        knownPersonalFrontierIdsByUserAndDimension = new HashMap<>();
        persistenceController = new DebouncedPersistenceController(
                TERRITORIES_UPDATE_SAVE_DEBOUNCE_MS,
                TERRITORIES_UPDATE_SAVE_MAX_DELAY_MS
        );
        frontierSettings = new FrontierSettings();
    }

    public void setSettings(FrontierSettings frontierSettings, long settingsRevision) {
        this.frontierSettings = new FrontierSettings(frontierSettings);
        this.settingsRevision = settingsRevision;
        if (ModDir != null) {
            saveSettingsData();
        }
    }

    public FrontierSettings getSettings() {
        return frontierSettings;
    }

    public long getSettingsRevision() {
        return settingsRevision;
    }

    public List<FrontierData> getAllGlobalFrontiers(ResourceKey<Level> dimension) {
        return dimensionsGlobalFrontiers.computeIfAbsent(dimension, k -> new ArrayList<>());
    }

    public List<FrontierData> getAllPersonalFrontiers(PlayerId user, ResourceKey<Level> dimension) {
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

    public List<CollectionData> getAllPersonalCollections(PlayerId user) {
        return usersPersonalCollections.computeIfAbsent(user, k -> new ArrayList<>());
    }

    public Iterable<FrontierData> iterateGlobalFrontiers() {
        return iterateNestedFrontiers(dimensionsGlobalFrontiers);
    }

    public Iterable<FrontierData> iteratePersonalFrontiers(PlayerId user) {
        return iterateNestedFrontiers(usersDimensionsPersonalFrontiers.getOrDefault(user, new HashMap<>()));
    }

    public Iterable<CollectionData> iterateGlobalCollections() {
        return globalCollections;
    }

    public Iterable<CollectionData> iteratePersonalCollections(PlayerId user) {
        List<CollectionData> collections = usersPersonalCollections.get(user);
        return collections != null ? collections : List.of();
    }

    Set<PlayerId> getReferencedPlayerIds() {
        LinkedHashSet<PlayerId> playerIds = new LinkedHashSet<>();

        for (FrontierData frontier : allFrontiers.values()) {
            PlayerReferenceCollector.add(playerIds, frontier);
        }

        for (CollectionData collection : allCollections.values()) {
            PlayerReferenceCollector.add(playerIds, collection);
        }

        PlayerReferenceCollector.add(playerIds, frontierSettings);

        return playerIds;
    }

    public List<FrontierData> getFrontiersInCollection(UUID collectionId) {
        LinkedHashSet<UUID> frontierIds = frontierIdsByCollectionId.get(collectionId);
        if (frontierIds == null || frontierIds.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<FrontierData> frontiers = new ArrayList<>();
        for (UUID frontierId : frontierIds) {
            FrontierData frontier = allFrontiers.get(frontierId);
            if (frontier != null && collectionId.equals(frontier.getCollectionId())) {
                frontiers.add(frontier);
            }
        }

        return frontiers;
    }

    public boolean userKnowsPersonalCollection(PlayerId user, UUID collectionId) {
        HashMap<ResourceKey<Level>, LinkedHashSet<UUID>> knownFrontierIdsByDimension = knownPersonalFrontierIdsByUserAndDimension.get(user);
        if (knownFrontierIdsByDimension == null) {
            return false;
        }

        for (LinkedHashSet<UUID> knownFrontierIds : knownFrontierIdsByDimension.values()) {
            for (UUID frontierId : knownFrontierIds) {
                FrontierData frontier = allFrontiers.get(frontierId);
                if (frontier != null && collectionId.equals(frontier.getCollectionId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public FrontierData createNewGlobalFrontier(FrontierCreateSpec createSpec) {
        List<FrontierData> frontiers = getAllGlobalFrontiers(createSpec.getDimension());
        return createNewFrontier(frontiers, createSpec);
    }

    public FrontierData createNewPersonalFrontier(FrontierCreateSpec createSpec) {
        List<FrontierData> frontiers = getAllPersonalFrontiers(createSpec.getOwner(), createSpec.getDimension());
        return createNewFrontier(frontiers, createSpec);
    }

    private FrontierData createNewFrontier(List<FrontierData> frontiers,
                                           FrontierCreateSpec createSpec) {
        FrontierData frontier = FrontierCreationFactory.createFrontier(createSpec);
        if (!frontier.getId().equals(createSpec.getFrontierId())) {
            throw new IllegalStateException("Created frontier id does not match the create spec");
        }

        frontier.setSharingRevision(0L);
        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);
        indexFrontier(frontier);
        markDirty();

        return frontier;
    }

    public void importPersonalFrontier(FrontierData frontier) {
        if (!frontier.getPersonal()) {
            return;
        }

        frontier.setSharingRevision(0L);
        List<FrontierData> frontiers = getAllPersonalFrontiers(frontier.getOwner(), frontier.getDimension());
        frontiers.add(frontier);
        allFrontiers.put(frontier.getId(), frontier);
        indexFrontier(frontier);
        markDirty();
    }

    public void addGlobalCollection(CollectionData collection) {
        if (collection.getPersonal()) {
            return;
        }

        collection.setCollectionRevision(0L);
        globalCollections.add(collection);
        allCollections.put(collection.getId(), collection);
        ensureCollectionIndexEntry(collection.getId());
        markDirty();
    }

    public void addPersonalCollection(CollectionData collection) {
        if (!collection.getPersonal()) {
            return;
        }

        collection.setCollectionRevision(0L);
        getAllPersonalCollections(collection.getOwner()).add(collection);
        allCollections.put(collection.getId(), collection);
        ensureCollectionIndexEntry(collection.getId());
        markDirty();
    }

    public void importPersonalCollection(CollectionData collection) {
        if (!collection.getPersonal()) {
            return;
        }

        collection.setCollectionRevision(0L);
        List<CollectionData> collections = getAllPersonalCollections(collection.getOwner());
        collections.add(collection);
        allCollections.put(collection.getId(), collection);
        ensureCollectionIndexEntry(collection.getId());
        markDirty();
    }

    public boolean deleteGlobalFrontier(ResourceKey<Level> dimension, UUID id) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(dimension);
        if (frontiers == null) {
            return false;
        }

        FrontierData frontier = allFrontiers.get(id);
        boolean deleted = frontiers.removeIf(x -> x.getId().equals(id));
        deleted |= allFrontiers.remove(id) != null;

        if (deleted) {
            if (frontier != null) {
                deindexFrontier(frontier);
            }
            markDirty();
        }

        return deleted;
    }

    public boolean deleteOwnedPersonalFrontier(PlayerId owner, ResourceKey<Level> dimension, UUID id) {
        FrontierData frontier = allFrontiers.get(id);
        if (frontier == null || !frontier.getPersonal() || !frontier.getOwner().equals(owner)) {
            return false;
        }

        if (!deletePersonalFrontierInternal(owner, dimension, id, false)) {
            return false;
        }

        if (frontier.getUserAccesses() != null) {
            for (FrontierUserAccess userShared : frontier.getUserAccesses()) {
                deletePersonalFrontierInternal(userShared.getPlayerId(), dimension, id, false);
            }
        }

        markDirty();
        return true;
    }

    public boolean addPendingPersonalFrontierShare(UUID frontierId, FrontierUserAccess userShared) {
        FrontierData frontier = allFrontiers.get(frontierId);
        if (frontier == null || !frontier.getPersonal() || frontier.hasUserAccess(userShared.getPlayerId())) {
            return false;
        }

        FrontierUserAccess pendingUserShared = new FrontierUserAccess(userShared);
        pendingUserShared.setPending(true);
        frontier.addUserAccess(pendingUserShared);
        frontier.advanceSharingRevision();
        markDirty();
        return true;
    }

    public boolean updatePersonalFrontierShare(UUID frontierId, FrontierUserAccess userShared) {
        FrontierData frontier = allFrontiers.get(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return false;
        }

        FrontierUserAccess currentUserShared = frontier.getUserAccess(userShared.getPlayerId());
        if (currentUserShared == null) {
            return false;
        }
        if (currentUserShared.getActions().equals(userShared.getActions())) {
            return false;
        }

        currentUserShared.setActions(userShared.getActions());
        frontier.advanceSharingRevision();
        markDirty();
        return true;
    }

    public boolean removePersonalFrontierShare(UUID frontierId, PlayerId targetUser) {
        FrontierData frontier = allFrontiers.get(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return false;
        }

        FrontierUserAccess userShared = frontier.getUserAccess(targetUser);
        if (userShared == null || userShared.getPlayerId().equals(frontier.getOwner())) {
            return false;
        }

        frontier.removeUserAccess(targetUser);
        if (!userShared.isPending()) {
            deletePersonalFrontierInternal(targetUser, frontier.getDimension(), frontierId, false);
        }

        frontier.advanceSharingRevision();
        markDirty();
        return true;
    }

    public boolean acceptPendingPersonalFrontierShare(PlayerId user, UUID frontierId) {
        FrontierData frontier = allFrontiers.get(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return false;
        }

        FrontierUserAccess userShared = frontier.getUserAccess(user);
        if (userShared == null || !userShared.isPending()) {
            return false;
        }

        if (!hasPersonalFrontier(user, frontierId)) {
            addPersonalFrontierReference(user, frontier);
        }

        userShared.setPending(false);
        frontier.advanceSharingRevision();
        markDirty();
        return true;
    }

    public boolean expirePendingPersonalFrontierShare(UUID frontierId, PlayerId targetUser) {
        FrontierData frontier = allFrontiers.get(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return false;
        }

        FrontierUserAccess userShared = frontier.getUserAccess(targetUser);
        if (userShared == null || !userShared.isPending()) {
            return false;
        }

        frontier.removeUserAccess(targetUser);
        frontier.advanceSharingRevision();
        markDirty();
        return true;
    }

    private boolean deletePersonalFrontierInternal(PlayerId user, ResourceKey<Level> dimension, UUID id, boolean markDirtyIfDeleted) {
        Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return false;
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(dimension);
        if (frontiers == null) {
            return false;
        }

        FrontierData frontier = allFrontiers.get(id);
        if (frontier != null && frontier.getOwner().equals(user)) {
            allFrontiers.remove(id);
            deindexFrontierCollection(frontier);
        }

        boolean deleted = frontiers.removeIf(x -> x.getId().equals(id));

        if (deleted) {
            deindexKnownPersonalFrontier(user, dimension, id);
            if (markDirtyIfDeleted) {
                markDirty();
            }
        }

        return deleted;
    }

    public FrontierChangeApplicationResult applyGlobalFrontierChange(UUID frontierId, FrontierChange change) {
        FrontierData frontier = allFrontiers.get(frontierId);
        if (frontier == null || frontier.getPersonal()) {
            return FrontierChangeApplicationResult.rejected("Global frontier not found");
        }

        FrontierChangeApplicationResult stagedResult = frontier.stageChange(change);
        if (!stagedResult.isApplied()) {
            return stagedResult;
        }

        FrontierIndexSnapshot previousState = captureFrontierIndexSnapshot(frontier);
        FrontierData stagedFrontier = Objects.requireNonNull(stagedResult.frontier());
        Date modified = new Date();
        stagedFrontier.setModified(modified);
        FrontierChange effectiveChange = stagedResult.effectiveChange();
        effectiveChange.setModifiedTime(modified.getTime());
        frontier.updateFromData(stagedFrontier);
        reindexFrontierAfterMutation(frontier, previousState);
        markDirty();
        return FrontierChangeApplicationResult.applied(frontier, effectiveChange);
    }

    public FrontierChangeApplicationResult applyPersonalFrontierChange(PlayerId user, UUID frontierId, FrontierChange change) {
        Map<ResourceKey<Level>, ArrayList<FrontierData>> dimensionsPersonalFrontiers = usersDimensionsPersonalFrontiers.get(user);
        if (dimensionsPersonalFrontiers == null) {
            return FrontierChangeApplicationResult.rejected("Personal frontier owner not found");
        }

        FrontierData frontier = allFrontiers.get(frontierId);
        if (frontier == null || !frontier.getPersonal()) {
            return FrontierChangeApplicationResult.rejected("Personal frontier not found");
        }

        List<FrontierData> frontiers = dimensionsPersonalFrontiers.get(frontier.getDimension());
        if (frontiers == null || frontiers.stream().noneMatch(existing -> existing.getId().equals(frontierId))) {
            return FrontierChangeApplicationResult.rejected("Personal frontier not found in owner dimension");
        }

        FrontierChangeApplicationResult stagedResult = frontier.stageChange(change);
        if (!stagedResult.isApplied()) {
            return stagedResult;
        }

        FrontierIndexSnapshot previousState = captureFrontierIndexSnapshot(frontier);
        FrontierData stagedFrontier = Objects.requireNonNull(stagedResult.frontier());
        Date modified = new Date();
        stagedFrontier.setModified(modified);
        FrontierChange effectiveChange = stagedResult.effectiveChange();
        effectiveChange.setModifiedTime(modified.getTime());
        frontier.updateFromData(stagedFrontier);
        reindexFrontierAfterMutation(frontier, previousState);
        markDirty();
        return FrontierChangeApplicationResult.applied(frontier, effectiveChange);
    }

    public boolean changePersonalFrontierToGlobal(PlayerId user, ResourceKey<Level> dimension, UUID id) {
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
            FrontierIndexSnapshot previousState = captureFrontierIndexSnapshot(frontier);
            if (frontier.getOwner().equals(user)) {
                if (frontier.getUserAccesses() != null) {
                    for (FrontierUserAccess userShared : frontier.getUserAccesses()) {
                        changePersonalFrontierToGlobal(userShared.getPlayerId(), dimension, id);
                    }
                }
                frontier.setPersonal(false);
                frontier.setCollectionId(null);
                frontier.setModified(new Date());
                frontier.removeAllUserAccesses();
                getAllGlobalFrontiers(dimension).add(frontier);
                reindexFrontierAfterMutation(frontier, previousState);
                markDirty();
            }
        }

        return deleted;
    }

    public boolean changeGlobalFrontierToPersonal(PlayerId newOwner, ResourceKey<Level> dimension, UUID id) {
        List<FrontierData> frontiers = dimensionsGlobalFrontiers.get(dimension);

        if (frontiers == null) {
            return false;
        }

        boolean deleted = frontiers.removeIf(x -> x.getId().equals(id));
        if (deleted) {
            FrontierData frontier = allFrontiers.get(id);
            FrontierIndexSnapshot previousState = captureFrontierIndexSnapshot(frontier);
            frontier.setPersonal(true);
            frontier.setCollectionId(null);
            frontier.setModified(new Date());
            frontier.setOwner(newOwner);
            getAllPersonalFrontiers(newOwner, dimension).add(frontier);
            reindexFrontierAfterMutation(frontier, previousState);
            markDirty();
        }

        return deleted;
    }

    public boolean hasPersonalFrontier(PlayerId user, UUID frontierID) {
        for (FrontierData frontier : iteratePersonalFrontiers(user)) {
            if (frontier.getId().equals(frontierID)) {
                return true;
            }
        }

        return false;
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
                    CompoundTag collectionTag = NbtReadHelper.requireCompound(allCollectionsTagList, i, "collections");
                    CollectionData.NbtReadResult result = CollectionData.readFromNBT(collectionTag, version, playerReferenceReadContext);
                    CollectionData collection = result.collection();
                    needBackup |= result.changedDuringLoad();
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
                    CompoundTag frontierTag = NbtReadHelper.requireCompound(allFrontiersTagList, i, "frontiers");
                    FrontierData.NbtReadResult result = FrontierData.readFromNBT(frontierTag, version, playerReferenceReadContext);
                    FrontierData frontier = result.frontier();
                    needBackup |= result.changedDuringLoad();
                    frontier.removePendingUserAccesses();
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

                        if (frontier.getUserAccesses() != null) {
                            for (FrontierUserAccess sharedUser : frontier.getUserAccesses()) {
                                getAllPersonalFrontiers(sharedUser.getPlayerId(), frontier.getDimension()).add(frontier);
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
        int skippedCollections = 0;
        for (CollectionData collection : allCollections.values()) {
            try {
                CompoundTag collectionTag = new CompoundTag();
                collection.writeToNBT(collectionTag, playerNames);
                allCollectionsTagList.add(collectionTag);
            } catch (RuntimeException e) {
                skippedCollections++;
                MapFrontiers.LOGGER.error("Skipping collection during server save because serialization failed. id={}, personal={}, lifetime={}",
                        collection.getId(), collection.getPersonal(), collection.getLifetime(), e);
            }
        }
        nbt.put("collections", allCollectionsTagList);

        ListTag allFrontiersTagList = new ListTag();
        int skippedFrontiers = 0;
        for (FrontierData frontier : allFrontiers.values()) {
            try {
                CompoundTag frontierTag = new CompoundTag();
                frontier.writeToNBT(frontierTag, playerNames);
                allFrontiersTagList.add(frontierTag);
            } catch (RuntimeException e) {
                skippedFrontiers++;
                MapFrontiers.LOGGER.error("Skipping frontier during server save because serialization failed. id={}, personal={}, lifetime={}",
                        frontier.getId(), frontier.getPersonal(), frontier.getLifetime(), e);
            }
        }
        nbt.put("frontiers", allFrontiersTagList);

        if (skippedCollections > 0 || skippedFrontiers > 0) {
            MapFrontiers.LOGGER.warn("Server save skipped invalid territories. savedCollections={}, skippedCollections={}, savedFrontiers={}, skippedFrontiers={}",
                    allCollectionsTagList.size(), skippedCollections, allFrontiersTagList.size(), skippedFrontiers);
        }

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
                saveTerritoriesSnapshot();
                persistenceController.markPersisted(System.currentTimeMillis());
                rebuildDerivedIndexes();
            } else {
                if (readFromNBT(nbtFrontiers)) {
                    NbtFileHelper.createBackup(ModDir, "frontiers.dat");
                    flushNow();
                } else {
                    persistenceController.markPersisted(System.currentTimeMillis());
                }

                rebuildDerivedIndexes();
            }

            CompoundTag nbtSettings = loadFile("settings.dat");
            if (nbtSettings.isEmpty()) {
                frontierSettings.resetToDefault();
                frontierSettings.writeToNBT(nbtSettings, playerNames);
                saveFile("settings.dat", nbtSettings);
            } else {
                if (frontierSettings.readFromNBT(nbtSettings, playerReferenceReadContext)) {
                    NbtFileHelper.createBackup(ModDir, "settings.dat");
                    saveSettingsData();
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    public void tickPersistence() {
        flushPendingScheduledTerritoriesSave();
    }

    public void markDirty() {
        persistenceController.markDirty(System.currentTimeMillis());
    }

    void markPlayerNameHintsDirty() {
        markDirty();
        if (ModDir != null) {
            saveSettingsData();
        }
    }

    public void flushTerritoriesOnShutdown() {
        if (persistenceController.hasPendingChanges()) {
            flushNow();
        }
    }

    public void flushNow() {
        saveTerritoriesSnapshot();
        persistenceController.markPersisted(System.currentTimeMillis());
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

        pruneCollectionIndexIfEmpty(collectionId);

        markDirty();
        return collection;
    }

    public void rebuildDerivedIndexes() {
        frontierIdsByCollectionId.clear();
        globalFrontierIdsByDimension.clear();
        knownPersonalFrontierIdsByUserAndDimension.clear();

        for (CollectionData collection : allCollections.values()) {
            ensureCollectionIndexEntry(collection.getId());
        }

        for (FrontierData frontier : allFrontiers.values()) {
            indexFrontier(frontier);
        }
    }

    private void indexFrontier(FrontierData frontier) {
        indexFrontierCollection(frontier);
        if (frontier.getPersonal()) {
            indexKnownUsers(frontier, collectKnownPersonalUsers(frontier));
        } else {
            indexGlobalFrontier(frontier);
        }
    }

    private void deindexFrontier(FrontierData frontier) {
        deindexFrontierCollection(frontier);
        if (frontier.getPersonal()) {
            deindexKnownUsers(frontier.getDimension(), frontier.getId(), collectKnownPersonalUsers(frontier));
        } else {
            deindexGlobalFrontier(frontier);
        }
    }

    private void indexFrontierCollection(FrontierData frontier) {
        if (!frontier.hasCollection()) {
            return;
        }

        frontierIdsByCollectionId.computeIfAbsent(frontier.getCollectionId(), key -> new LinkedHashSet<>()).add(frontier.getId());
    }

    private void deindexFrontierCollection(FrontierData frontier) {
        if (!frontier.hasCollection()) {
            return;
        }

        LinkedHashSet<UUID> frontierIds = frontierIdsByCollectionId.get(frontier.getCollectionId());
        if (frontierIds == null) {
            return;
        }

        frontierIds.remove(frontier.getId());
        pruneCollectionIndexIfEmpty(frontier.getCollectionId());
    }

    private void ensureCollectionIndexEntry(UUID collectionId) {
        frontierIdsByCollectionId.computeIfAbsent(collectionId, key -> new LinkedHashSet<>());
    }

    private void pruneCollectionIndexIfEmpty(UUID collectionId) {
        LinkedHashSet<UUID> frontierIds = frontierIdsByCollectionId.get(collectionId);
        if (frontierIds != null && frontierIds.isEmpty()) {
            frontierIdsByCollectionId.remove(collectionId);
        }
    }

    private void indexGlobalFrontier(FrontierData frontier) {
        globalFrontierIdsByDimension.computeIfAbsent(frontier.getDimension(), key -> new LinkedHashSet<>()).add(frontier.getId());
    }

    private void deindexGlobalFrontier(FrontierData frontier) {
        LinkedHashSet<UUID> frontierIds = globalFrontierIdsByDimension.get(frontier.getDimension());
        if (frontierIds == null) {
            return;
        }

        frontierIds.remove(frontier.getId());
        if (frontierIds.isEmpty()) {
            globalFrontierIdsByDimension.remove(frontier.getDimension());
        }
    }

    private void indexKnownPersonalFrontier(PlayerId user, FrontierData frontier) {
        knownPersonalFrontierIdsByUserAndDimension
                .computeIfAbsent(user, key -> new HashMap<>())
                .computeIfAbsent(frontier.getDimension(), key -> new LinkedHashSet<>())
                .add(frontier.getId());
    }

    private void addPersonalFrontierReference(PlayerId user, FrontierData frontier) {
        List<FrontierData> frontiers = getAllPersonalFrontiers(user, frontier.getDimension());
        frontiers.add(frontier);
        indexKnownPersonalFrontier(user, frontier);
    }

    private void deindexKnownPersonalFrontier(PlayerId user, ResourceKey<Level> dimension, UUID frontierId) {
        HashMap<ResourceKey<Level>, LinkedHashSet<UUID>> dimensionsFrontierIds = knownPersonalFrontierIdsByUserAndDimension.get(user);
        if (dimensionsFrontierIds == null) {
            return;
        }

        LinkedHashSet<UUID> frontierIds = dimensionsFrontierIds.get(dimension);
        if (frontierIds == null) {
            return;
        }

        frontierIds.remove(frontierId);
        if (frontierIds.isEmpty()) {
            dimensionsFrontierIds.remove(dimension);
        }

        if (dimensionsFrontierIds.isEmpty()) {
            knownPersonalFrontierIdsByUserAndDimension.remove(user);
        }
    }

    private void indexKnownUsers(FrontierData frontier, LinkedHashSet<PlayerId> knownUsers) {
        for (PlayerId knownUser : knownUsers) {
            indexKnownPersonalFrontier(knownUser, frontier);
        }
    }

    private void deindexKnownUsers(ResourceKey<Level> dimension, UUID frontierId, LinkedHashSet<PlayerId> knownUsers) {
        for (PlayerId knownUser : knownUsers) {
            deindexKnownPersonalFrontier(knownUser, dimension, frontierId);
        }
    }

    private FrontierIndexSnapshot captureFrontierIndexSnapshot(FrontierData frontier) {
        return new FrontierIndexSnapshot(
                frontier.getCollectionId(),
                frontier.getDimension(),
                frontier.getPersonal(),
                collectKnownPersonalUsers(frontier)
        );
    }

    private void reindexFrontierAfterMutation(FrontierData frontier, FrontierIndexSnapshot previousState) {
        UUID previousCollectionId = previousState.collectionId();
        UUID currentCollectionId = frontier.getCollectionId();
        if (!Objects.equals(previousCollectionId, currentCollectionId)) {
            if (previousCollectionId != null) {
                LinkedHashSet<UUID> frontierIds = frontierIdsByCollectionId.get(previousCollectionId);
                if (frontierIds != null) {
                    frontierIds.remove(frontier.getId());
                    pruneCollectionIndexIfEmpty(previousCollectionId);
                }
            }

            if (currentCollectionId != null) {
                ensureCollectionIndexEntry(currentCollectionId);
                frontierIdsByCollectionId.get(currentCollectionId).add(frontier.getId());
            }
        }

        if (previousState.personal()) {
            LinkedHashSet<PlayerId> currentKnownUsers = frontier.getPersonal()
                    ? collectKnownPersonalUsers(frontier)
                    : new LinkedHashSet<>();
            boolean knowledgeChanged = !previousState.dimension().equals(frontier.getDimension())
                    || previousState.personal() != frontier.getPersonal()
                    || !previousState.knownUsers().equals(currentKnownUsers);
            if (knowledgeChanged) {
                deindexKnownUsers(previousState.dimension(), frontier.getId(), previousState.knownUsers());
                if (frontier.getPersonal()) {
                    indexKnownUsers(frontier, currentKnownUsers);
                } else {
                    indexGlobalFrontier(frontier);
                }
            }
            return;
        }

        if (frontier.getPersonal()) {
            deindexGlobalFrontier(previousState.dimension(), frontier.getId());
            indexKnownUsers(frontier, collectKnownPersonalUsers(frontier));
            return;
        }

        if (!previousState.dimension().equals(frontier.getDimension())) {
            deindexGlobalFrontier(previousState.dimension(), frontier.getId());
            indexGlobalFrontier(frontier);
        }
    }

    private LinkedHashSet<PlayerId> collectKnownPersonalUsers(FrontierData frontier) {
        LinkedHashSet<PlayerId> knownUsers = new LinkedHashSet<>();
        if (!frontier.getPersonal()) {
            return knownUsers;
        }

        knownUsers.add(frontier.getOwner());
        if (frontier.getUserAccesses() != null) {
            for (FrontierUserAccess userShared : frontier.getUserAccesses()) {
                if (!userShared.isPending()) {
                    knownUsers.add(userShared.getPlayerId());
                }
            }
        }

        return knownUsers;
    }

    private void deindexGlobalFrontier(ResourceKey<Level> dimension, UUID frontierId) {
        LinkedHashSet<UUID> frontierIds = globalFrontierIdsByDimension.get(dimension);
        if (frontierIds == null) {
            return;
        }

        frontierIds.remove(frontierId);
        if (frontierIds.isEmpty()) {
            globalFrontierIdsByDimension.remove(dimension);
        }
    }

    private record FrontierIndexSnapshot(@Nullable UUID collectionId,
                                         ResourceKey<Level> dimension,
                                         boolean personal,
                                         LinkedHashSet<PlayerId> knownUsers) {
    }

    private static Iterable<FrontierData> iterateNestedFrontiers(Map<ResourceKey<Level>, ? extends List<FrontierData>> frontiersByDimension) {
        return () -> frontiersByDimension.values().stream().flatMap(List::stream).iterator();
    }

    private void flushPendingScheduledTerritoriesSave() {
        long now = System.currentTimeMillis();
        if (persistenceController.shouldFlushOnTick(now)) {
            saveTerritoriesSnapshot();
            persistenceController.markPersisted(now);
        }
    }

    private void saveTerritoriesSnapshot() {
        CompoundTag nbtFrontiers = new CompoundTag();
        writeToNBT(nbtFrontiers);
        saveFile("frontiers.dat", nbtFrontiers);
    }

    private void saveSettingsData() {
        CompoundTag nbtSettings = new CompoundTag();
        frontierSettings.writeToNBT(nbtSettings, playerNames);
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
