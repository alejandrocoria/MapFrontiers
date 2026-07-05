package games.alejandrocoria.mapfrontiers.client.territory.collection;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.territory.ClientMapFrontiersStorageHelper;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityMask;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtCompat;
import games.alejandrocoria.mapfrontiers.common.util.NbtFileHelper;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CollectionLocalOverrides {
    private static final String FILENAME = "collection_overrides.dat";
    private static final int COLLECTION_OVERRIDES_DATA_VERSION = 13;

    private final Map<UUID, Pair<CollectionVisibilityData, CollectionVisibilityMask>> overrides = new HashMap<>();
    private File ModDir;

    public CollectionLocalOverrides() {
        loadData();
    }

    public Pair<CollectionVisibilityData, CollectionVisibilityMask> getVisibility(UUID id) {
        var override = overrides.get(id);
        if (override == null) {
            return Pair.of(new CollectionVisibilityData(), new CollectionVisibilityMask());
        }
        return override;
    }

    public void setVisibility(UUID id, Pair<CollectionVisibilityData, CollectionVisibilityMask> visibility) {
        if (visibility.second().hasAny()) {
            overrides.put(id, visibility);
        } else {
            overrides.remove(id);
        }
        saveData();
    }

    public static CollectionVisibilityData resolveVisibility(CollectionVisibilityData baseVisibility,
                                                             Pair<CollectionVisibilityData, CollectionVisibilityMask> visibilityOverride) {
        CollectionVisibilityData resolvedVisibility = new CollectionVisibilityData(baseVisibility);
        resolvedVisibility.applyOverride(visibilityOverride.first(), visibilityOverride.second());
        return resolvedVisibility;
    }

    private boolean readFromNBT(CompoundTag nbt) {
        boolean needBackup = false;
        try {
            int version = NbtCompat.getIntOr(nbt, "Version", 0);
            if (version < COLLECTION_OVERRIDES_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Unsupported legacy data version in {}: {}. The mod uses {}", FILENAME, version, COLLECTION_OVERRIDES_DATA_VERSION);
                return true;
            } else if (version > COLLECTION_OVERRIDES_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in {} higher than expected. The mod uses {}", FILENAME, COLLECTION_OVERRIDES_DATA_VERSION);
                needBackup = true;
            }

            ListTag overridesTagList = NbtCompat.getListOrEmpty(nbt, "overrides");
            for (int i = 0; i < overridesTagList.size(); ++i) {
                try {
                    CompoundTag overrideTag = NbtReadHelper.requireCompound(overridesTagList, i, "overrides");
                    UUID id = UUID.fromString(NbtReadHelper.requireString(overrideTag, "id"));

                    CompoundTag visibilityTag = NbtReadHelper.requireCompound(overrideTag, "visibility");
                    Pair<CollectionVisibilityData, CollectionVisibilityMask> override = readSparseOverrideData(visibilityTag);
                    CollectionVisibilityMask mask = override.second();
                    if (mask.hasAny()) {
                        overrides.put(id, override);
                    }
                } catch (InvalidNbtFormatException e) {
                    MapFrontiers.LOGGER.warn("Skipping invalid collection override at overrides[{}]: {}", i, e.getMessage());
                    needBackup = true;
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.warn("Failed to read {}: {}", FILENAME, e.getMessage());
            return true;
        }

        return needBackup;
    }

    private void writeToNBT(CompoundTag nbt) {
        ListTag overridesTagList = new ListTag();
        for (Map.Entry<UUID, Pair<CollectionVisibilityData, CollectionVisibilityMask>> override : overrides.entrySet()) {
            if (!override.getValue().second().hasAny()) {
                continue;
            }

            CompoundTag overrideTag = new CompoundTag();
            overrideTag.putString("id", override.getKey().toString());

            CompoundTag visibilityTag = writeSparseOverrideData(override.getValue().first(), override.getValue().second());
            overrideTag.put("visibility", visibilityTag);

            overridesTagList.add(overrideTag);
        }
        nbt.put("overrides", overridesTagList);
        nbt.putInt("Version", COLLECTION_OVERRIDES_DATA_VERSION);
    }

    private void loadData() {
        try {
            Minecraft client = Minecraft.getInstance();
            File journeyMapModDir = ClientMapFrontiersStorageHelper.resolveJourneyMapModDir(client);
            File singleplayerModDir = ClientMapFrontiersStorageHelper.resolveSingleplayerModDir(client);
            if (singleplayerModDir != null) {
                ModDir = singleplayerModDir;
                migrateSingleplayerData(journeyMapModDir, singleplayerModDir);
            } else {
                if (client.isLocalServer()) {
                    MapFrontiers.LOGGER.warn("Singleplayer server unavailable while resolving {} storage. Falling back to JourneyMap directory.", FILENAME);
                }
                ModDir = journeyMapModDir;
            }

            CompoundTag nbtOverrides = loadFile(FILENAME);
            if (!nbtOverrides.isEmpty()) {
                if (readFromNBT(nbtOverrides)) {
                    NbtFileHelper.createBackup(ModDir, FILENAME);
                    saveData();
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error(e.getMessage(), e);
        }
    }

    private void saveData() {
        if (ModDir != null) {
            CompoundTag nbtOverrides = new CompoundTag();
            writeToNBT(nbtOverrides);
            saveFile(FILENAME, nbtOverrides);
        }
    }

    private void migrateSingleplayerData(File journeyMapModDir, File singleplayerModDir) {
        if (!NbtFileHelper.hasRelatedFiles(journeyMapModDir, FILENAME)) {
            return;
        }

        if (!NbtFileHelper.hasRelatedFiles(singleplayerModDir, FILENAME)) {
            NbtFileHelper.moveRelatedFiles(journeyMapModDir, singleplayerModDir, FILENAME);
            MapFrontiers.LOGGER.info("Moved {} data from JourneyMap directory to singleplayer world directory.", FILENAME);
            return;
        }

        File conflictDir = NbtFileHelper.moveRelatedFilesToConflictDir(journeyMapModDir,
                new File(journeyMapModDir, "migration_conflicts"), FILENAME);
        MapFrontiers.LOGGER.warn("Detected conflicting {} data in JourneyMap and singleplayer world directories. Preserved JourneyMap files at {}",
                FILENAME, conflictDir);
    }

    private void saveFile(String filename, CompoundTag nbt) {
        NbtFileHelper.saveCompressedNbtSafely(ModDir, filename, nbt);
    }

    private CompoundTag loadFile(String filename) {
        File f = new File(ModDir, filename);
        if (f.exists()) {
            try (FileInputStream inputStream = new FileInputStream(f)) {
                return NbtIo.readCompressed(inputStream);
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        }

        return new CompoundTag();
    }

    private Pair<CollectionVisibilityData, CollectionVisibilityMask> readSparseOverrideData(CompoundTag dataTag) {
        CollectionVisibilityData data = new CollectionVisibilityData();
        CollectionVisibilityMask mask = new CollectionVisibilityMask();
        data.readSparseNbt(dataTag, mask);
        return Pair.of(data, mask);
    }

    private CompoundTag writeSparseOverrideData(CollectionVisibilityData data, CollectionVisibilityMask mask) {
        CompoundTag dataTag = new CompoundTag();
        data.writeSparseNbt(dataTag, mask);
        return dataTag;
    }
}
