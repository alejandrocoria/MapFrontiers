package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.territory.ClientMapFrontiersStorageHelper;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierVisibilityMask;
import games.alejandrocoria.mapfrontiers.common.territory.VisibilityData;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtFileHelper;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FrontierLocalOverrides {
    private static final String FILENAME = "frontier_overrides.dat";
    private static final int FRONTIER_OVERRIDES_DATA_VERSION = 13;

    private final Map<UUID, Pair<VisibilityData, FrontierVisibilityMask>> overrides = new HashMap<>();
    private File ModDir;

    public FrontierLocalOverrides() {
        loadData();
    }

    public Pair<VisibilityData, FrontierVisibilityMask> getVisibility(UUID id) {
        var override = overrides.get(id);
        if (override == null) {
            return Pair.of(new VisibilityData(), new FrontierVisibilityMask());
        }
        return override;
    }

    public void setVisibility(UUID id, Pair<VisibilityData, FrontierVisibilityMask> visibility) {
        if (visibility.second().hasAny()) {
            overrides.put(id, visibility);
        } else {
            overrides.remove(id);
        }
        saveData();
    }

    public static VisibilityData resolveVisibility(VisibilityData baseVisibility, Pair<VisibilityData, FrontierVisibilityMask> visibilityOverride) {
        VisibilityData resolvedVisibility = new VisibilityData(baseVisibility);
        resolvedVisibility.applyOverride(visibilityOverride.first(), visibilityOverride.second());
        return resolvedVisibility;
    }

    private boolean readFromNBT(CompoundTag nbt) {
        boolean needBackup = false;
        try {
            int version = nbt.getIntOr("Version", 0);
            if (version == 0) {
                MapFrontiers.LOGGER.warn("Data version in {} not found, expected {}", FILENAME, FRONTIER_OVERRIDES_DATA_VERSION);
                needBackup = true;
            } else if (version > FRONTIER_OVERRIDES_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in {} higher than expected. The mod uses {}", FILENAME, FRONTIER_OVERRIDES_DATA_VERSION);
                needBackup = true;
            }

            ListTag overridesTagList;
            if (version < FRONTIER_OVERRIDES_DATA_VERSION) {
                overridesTagList = migrateLegacyOverrides(nbt.getListOrEmpty("overrides"));
                nbt.put("overrides", overridesTagList);
                nbt.putInt("Version", FRONTIER_OVERRIDES_DATA_VERSION);
                needBackup = true;
            } else {
                overridesTagList = nbt.getListOrEmpty("overrides");
            }

            for (int i = 0; i < overridesTagList.size(); ++i) {
                try {
                    CompoundTag overrideTag = NbtReadHelper.requireCompound(overridesTagList, i, "overrides");
                    UUID id = UUID.fromString(NbtReadHelper.requireString(overrideTag, "id"));

                    CompoundTag visibilityTag = NbtReadHelper.requireCompound(overrideTag, "visibility");
                    Pair<VisibilityData, FrontierVisibilityMask> override = readSparseOverrideData(visibilityTag);
                    FrontierVisibilityMask mask = override.second();
                    if (mask.hasAny()) {
                        overrides.put(id, override);
                    }
                } catch (InvalidNbtFormatException e) {
                    MapFrontiers.LOGGER.warn("Skipping invalid frontier override at overrides[{}]: {}", i, e.getMessage());
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
        for (Map.Entry<UUID, Pair<VisibilityData, FrontierVisibilityMask>> override : overrides.entrySet()) {
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

        nbt.putInt("Version", FRONTIER_OVERRIDES_DATA_VERSION);
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

            CompoundTag nbtFrontiers = loadFile(FILENAME);
            if (!nbtFrontiers.isEmpty()) {
                if (readFromNBT(nbtFrontiers)) {
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
            CompoundTag nbtFrontiers = new CompoundTag();
            writeToNBT(nbtFrontiers);
            saveFile(FILENAME, nbtFrontiers);
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
                return NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            } catch (Exception e) {
                MapFrontiers.LOGGER.error(e.getMessage(), e);
            }
        }

        return new CompoundTag();
    }

    private ListTag migrateLegacyOverrides(ListTag legacyOverridesTagList) {
        ListTag migratedOverridesTagList = new ListTag();
        for (int i = 0; i < legacyOverridesTagList.size(); ++i) {
            try {
                CompoundTag legacyOverrideTag = NbtReadHelper.requireCompound(legacyOverridesTagList, i, "overrides");
                UUID id = UUID.fromString(NbtReadHelper.requireString(legacyOverrideTag, "id"));

                CompoundTag legacyDataTag = NbtReadHelper.requireCompound(legacyOverrideTag, "data");
                VisibilityData legacyData = new VisibilityData();
                legacyData.readFromNBT(legacyDataTag);

                CompoundTag legacyMaskTag = NbtReadHelper.requireCompound(legacyOverrideTag, "mask");
                VisibilityData legacyMask = new VisibilityData(false);
                legacyMask.readFromNBT(legacyMaskTag);
                FrontierVisibilityMask migratedMask = new FrontierVisibilityMask(legacyMask);

                if (!migratedMask.hasAny()) {
                    continue;
                }

                CompoundTag migratedOverrideTag = new CompoundTag();
                migratedOverrideTag.putString("id", id.toString());
                migratedOverrideTag.put("visibility", writeSparseOverrideData(legacyData, migratedMask));
                migratedOverridesTagList.add(migratedOverrideTag);
            } catch (InvalidNbtFormatException e) {
                MapFrontiers.LOGGER.warn("Skipping invalid frontier override at overrides[{}]: {}", i, e.getMessage());
            }
        }

        return migratedOverridesTagList;
    }

    private Pair<VisibilityData, FrontierVisibilityMask> readSparseOverrideData(CompoundTag dataTag) {
        VisibilityData data = new VisibilityData();
        FrontierVisibilityMask mask = new FrontierVisibilityMask();
        data.readSparseNbt(dataTag, mask);
        return Pair.of(data, mask);
    }

    private CompoundTag writeSparseOverrideData(VisibilityData data, FrontierVisibilityMask mask) {
        CompoundTag dataTag = new CompoundTag();
        data.writeSparseNbt(dataTag, mask);
        return dataTag;
    }
}
