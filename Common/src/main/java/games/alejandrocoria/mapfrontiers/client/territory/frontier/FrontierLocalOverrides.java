package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.territory.ClientMapFrontiersStorageHelper;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierVisibility;
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

    private final Map<UUID, Pair<VisibilityData, VisibilityData>> overrides = new HashMap<>();
    private File ModDir;

    public FrontierLocalOverrides() {
        loadData();
    }

    public Pair<VisibilityData, VisibilityData> getVisibility(UUID id) {
        var override = overrides.get(id);
        if (override == null) {
            return Pair.of(new VisibilityData(), new VisibilityData(false));
        }
        return override;
    }

    public void setVisibility(UUID id, Pair<VisibilityData, VisibilityData> visibility) {
        if (visibility.second().hasSome()) {
            overrides.put(id, visibility);
        } else {
            overrides.remove(id);
        }
        saveData();
    }

    public static VisibilityData resolveVisibility(VisibilityData baseVisibility, Pair<VisibilityData, VisibilityData> visibilityOverride) {
        VisibilityData resolvedVisibility = new VisibilityData(baseVisibility);
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            if (visibilityOverride.second().getValue(visibility)) {
                resolvedVisibility.setValue(visibility, visibilityOverride.first().getValue(visibility));
            }
        }

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
                    Pair<VisibilityData, VisibilityData> override = readSparseOverrideData(visibilityTag);
                    VisibilityData mask = override.second();
                    if (mask.hasSome()) {
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
        for (Map.Entry<UUID, Pair<VisibilityData, VisibilityData>> override : overrides.entrySet()) {
            if (!override.getValue().second().hasSome()) {
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

                if (!legacyMask.hasSome()) {
                    continue;
                }

                CompoundTag migratedOverrideTag = new CompoundTag();
                migratedOverrideTag.putString("id", id.toString());
                migratedOverrideTag.put("visibility", writeSparseOverrideData(legacyData, legacyMask));
                migratedOverridesTagList.add(migratedOverrideTag);
            } catch (InvalidNbtFormatException e) {
                MapFrontiers.LOGGER.warn("Skipping invalid frontier override at overrides[{}]: {}", i, e.getMessage());
            }
        }

        return migratedOverridesTagList;
    }

    private Pair<VisibilityData, VisibilityData> readSparseOverrideData(CompoundTag dataTag) {
        VisibilityData data = new VisibilityData();
        VisibilityData mask = new VisibilityData(false);

        readSparseVisibilityValue(dataTag, "visible", FrontierVisibility.Frontier, data, mask);
        readSparseVisibilityValue(dataTag, "announceInChat", FrontierVisibility.AnnounceInChat, data, mask);
        readSparseVisibilityValue(dataTag, "announceInTitle", FrontierVisibility.AnnounceInTitle, data, mask);
        readSparseVisibilityValue(dataTag, "mentionCollection", FrontierVisibility.MentionCollection, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenVisible", FrontierVisibility.Fullscreen, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenNameVisible", FrontierVisibility.FullscreenName, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenCollectionVisible", FrontierVisibility.FullscreenCollection, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenOwnerVisible", FrontierVisibility.FullscreenOwner, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenBannerVisible", FrontierVisibility.FullscreenBanner, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenDay", FrontierVisibility.FullscreenDay, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenNight", FrontierVisibility.FullscreenNight, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenUnderground", FrontierVisibility.FullscreenUnderground, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenTopo", FrontierVisibility.FullscreenTopo, data, mask);
        readSparseVisibilityValue(dataTag, "fullscreenBiome", FrontierVisibility.FullscreenBiome, data, mask);
        readSparseVisibilityValue(dataTag, "minimapVisible", FrontierVisibility.Minimap, data, mask);
        readSparseVisibilityValue(dataTag, "minimapNameVisible", FrontierVisibility.MinimapName, data, mask);
        readSparseVisibilityValue(dataTag, "minimapCollectionVisible", FrontierVisibility.MinimapCollection, data, mask);
        readSparseVisibilityValue(dataTag, "minimapOwnerVisible", FrontierVisibility.MinimapOwner, data, mask);
        readSparseVisibilityValue(dataTag, "minimapBannerVisible", FrontierVisibility.MinimapBanner, data, mask);
        readSparseVisibilityValue(dataTag, "minimapDay", FrontierVisibility.MinimapDay, data, mask);
        readSparseVisibilityValue(dataTag, "minimapNight", FrontierVisibility.MinimapNight, data, mask);
        readSparseVisibilityValue(dataTag, "minimapUnderground", FrontierVisibility.MinimapUnderground, data, mask);
        readSparseVisibilityValue(dataTag, "minimapTopo", FrontierVisibility.MinimapTopo, data, mask);
        readSparseVisibilityValue(dataTag, "minimapBiome", FrontierVisibility.MinimapBiome, data, mask);
        readSparseVisibilityValue(dataTag, "webmapVisible", FrontierVisibility.Webmap, data, mask);
        readSparseVisibilityValue(dataTag, "webmapNameVisible", FrontierVisibility.WebmapName, data, mask);
        readSparseVisibilityValue(dataTag, "webmapCollectionVisible", FrontierVisibility.WebmapCollection, data, mask);
        readSparseVisibilityValue(dataTag, "webmapOwnerVisible", FrontierVisibility.WebmapOwner, data, mask);
        readSparseVisibilityValue(dataTag, "webmapBannerVisible", FrontierVisibility.WebmapBanner, data, mask);
        readSparseVisibilityValue(dataTag, "webmapDay", FrontierVisibility.WebmapDay, data, mask);
        readSparseVisibilityValue(dataTag, "webmapNight", FrontierVisibility.WebmapNight, data, mask);
        readSparseVisibilityValue(dataTag, "webmapUnderground", FrontierVisibility.WebmapUnderground, data, mask);
        readSparseVisibilityValue(dataTag, "webmapTopo", FrontierVisibility.WebmapTopo, data, mask);
        readSparseVisibilityValue(dataTag, "webmapBiome", FrontierVisibility.WebmapBiome, data, mask);

        return Pair.of(data, mask);
    }

    private CompoundTag writeSparseOverrideData(VisibilityData data, VisibilityData mask) {
        CompoundTag dataTag = new CompoundTag();

        writeSparseVisibilityValue(dataTag, "visible", FrontierVisibility.Frontier, data, mask);
        writeSparseVisibilityValue(dataTag, "announceInChat", FrontierVisibility.AnnounceInChat, data, mask);
        writeSparseVisibilityValue(dataTag, "announceInTitle", FrontierVisibility.AnnounceInTitle, data, mask);
        writeSparseVisibilityValue(dataTag, "mentionCollection", FrontierVisibility.MentionCollection, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenVisible", FrontierVisibility.Fullscreen, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenNameVisible", FrontierVisibility.FullscreenName, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenCollectionVisible", FrontierVisibility.FullscreenCollection, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenOwnerVisible", FrontierVisibility.FullscreenOwner, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenBannerVisible", FrontierVisibility.FullscreenBanner, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenDay", FrontierVisibility.FullscreenDay, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenNight", FrontierVisibility.FullscreenNight, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenUnderground", FrontierVisibility.FullscreenUnderground, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenTopo", FrontierVisibility.FullscreenTopo, data, mask);
        writeSparseVisibilityValue(dataTag, "fullscreenBiome", FrontierVisibility.FullscreenBiome, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapVisible", FrontierVisibility.Minimap, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapNameVisible", FrontierVisibility.MinimapName, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapCollectionVisible", FrontierVisibility.MinimapCollection, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapOwnerVisible", FrontierVisibility.MinimapOwner, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapBannerVisible", FrontierVisibility.MinimapBanner, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapDay", FrontierVisibility.MinimapDay, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapNight", FrontierVisibility.MinimapNight, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapUnderground", FrontierVisibility.MinimapUnderground, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapTopo", FrontierVisibility.MinimapTopo, data, mask);
        writeSparseVisibilityValue(dataTag, "minimapBiome", FrontierVisibility.MinimapBiome, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapVisible", FrontierVisibility.Webmap, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapNameVisible", FrontierVisibility.WebmapName, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapCollectionVisible", FrontierVisibility.WebmapCollection, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapOwnerVisible", FrontierVisibility.WebmapOwner, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapBannerVisible", FrontierVisibility.WebmapBanner, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapDay", FrontierVisibility.WebmapDay, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapNight", FrontierVisibility.WebmapNight, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapUnderground", FrontierVisibility.WebmapUnderground, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapTopo", FrontierVisibility.WebmapTopo, data, mask);
        writeSparseVisibilityValue(dataTag, "webmapBiome", FrontierVisibility.WebmapBiome, data, mask);

        return dataTag;
    }

    private void readSparseVisibilityValue(CompoundTag dataTag, String key, FrontierVisibility visibility,
                                           VisibilityData data, VisibilityData mask) {
        if (!dataTag.contains(key)) {
            return;
        }

        data.setValue(visibility, dataTag.getBooleanOr(key, false));
        mask.setValue(visibility, true);
    }

    private void writeSparseVisibilityValue(CompoundTag dataTag, String key, FrontierVisibility visibility,
                                            VisibilityData data, VisibilityData mask) {
        if (!mask.getValue(visibility)) {
            return;
        }

        dataTag.putBoolean(key, data.getValue(visibility));
    }
}
