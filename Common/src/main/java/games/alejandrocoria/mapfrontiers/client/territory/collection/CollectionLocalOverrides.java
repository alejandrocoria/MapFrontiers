package games.alejandrocoria.mapfrontiers.client.territory.collection;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.territory.ClientMapFrontiersStorageHelper;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionVisibilityData;
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
        if (visibility.second().hasSome()) {
            overrides.put(id, visibility);
        } else {
            overrides.remove(id);
        }
        saveData();
    }

    public static CollectionVisibilityData resolveVisibility(CollectionVisibilityData baseVisibility,
                                                             Pair<CollectionVisibilityData, CollectionVisibilityMask> visibilityOverride) {
        CollectionVisibilityData resolvedVisibility = new CollectionVisibilityData(baseVisibility);
        CollectionVisibilityData overrideVisibility = visibilityOverride.first();
        CollectionVisibilityMask overrideMask = visibilityOverride.second();

        applyMaskedBoolean(overrideMask.isVisible(), overrideVisibility.isVisible(), resolvedVisibility::setVisible);
        applyMaskedInt(overrideMask.getFullscreenZoom(), overrideVisibility.getFullscreenZoom(), resolvedVisibility::setFullscreenZoom);
        applyMaskedInt(overrideMask.getMinimapZoom(), overrideVisibility.getMinimapZoom(), resolvedVisibility::setMinimapZoom);
        applyMaskedInt(overrideMask.getWebmapZoom(), overrideVisibility.getWebmapZoom(), resolvedVisibility::setWebmapZoom);
        applyMaskedBoolean(overrideMask.getFullscreenName(), overrideVisibility.getFullscreenName(), resolvedVisibility::setFullscreenName);
        applyMaskedBoolean(overrideMask.getFullscreenOwner(), overrideVisibility.getFullscreenOwner(), resolvedVisibility::setFullscreenOwner);
        applyMaskedBoolean(overrideMask.getFullscreenBanner(), overrideVisibility.getFullscreenBanner(), resolvedVisibility::setFullscreenBanner);
        applyMaskedBoolean(overrideMask.getMinimapName(), overrideVisibility.getMinimapName(), resolvedVisibility::setMinimapName);
        applyMaskedBoolean(overrideMask.getMinimapOwner(), overrideVisibility.getMinimapOwner(), resolvedVisibility::setMinimapOwner);
        applyMaskedBoolean(overrideMask.getMinimapBanner(), overrideVisibility.getMinimapBanner(), resolvedVisibility::setMinimapBanner);
        applyMaskedBoolean(overrideMask.getWebmapName(), overrideVisibility.getWebmapName(), resolvedVisibility::setWebmapName);
        applyMaskedBoolean(overrideMask.getWebmapOwner(), overrideVisibility.getWebmapOwner(), resolvedVisibility::setWebmapOwner);
        applyMaskedBoolean(overrideMask.getWebmapBanner(), overrideVisibility.getWebmapBanner(), resolvedVisibility::setWebmapBanner);

        return resolvedVisibility;
    }

    private boolean readFromNBT(CompoundTag nbt) {
        boolean needBackup = false;
        try {
            int version = nbt.getIntOr("Version", 0);
            if (version == 0) {
                MapFrontiers.LOGGER.warn("Data version in {} not found, expected {}", FILENAME, COLLECTION_OVERRIDES_DATA_VERSION);
                needBackup = true;
            } else if (version > COLLECTION_OVERRIDES_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in {} higher than expected. The mod uses {}", FILENAME, COLLECTION_OVERRIDES_DATA_VERSION);
                needBackup = true;
            }

            ListTag overridesTagList;
            if (version < COLLECTION_OVERRIDES_DATA_VERSION) {
                overridesTagList = migrateLegacyOverrides(nbt.getListOrEmpty("overrides"));
                nbt.put("overrides", overridesTagList);
                nbt.putInt("Version", COLLECTION_OVERRIDES_DATA_VERSION);
                needBackup = true;
            } else {
                overridesTagList = nbt.getListOrEmpty("overrides");
            }

            for (int i = 0; i < overridesTagList.size(); ++i) {
                try {
                    CompoundTag overrideTag = NbtReadHelper.requireCompound(overridesTagList, i, "overrides");
                    UUID id = UUID.fromString(NbtReadHelper.requireString(overrideTag, "id"));

                    CompoundTag visibilityTag = NbtReadHelper.requireCompound(overrideTag, "visibility");
                    Pair<CollectionVisibilityData, CollectionVisibilityMask> override = readSparseOverrideData(visibilityTag);
                    CollectionVisibilityMask mask = override.second();
                    if (mask.hasSome()) {
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
                CollectionVisibilityData legacyData = new CollectionVisibilityData();
                legacyData.readFromNBT(legacyDataTag);

                CompoundTag legacyMaskTag = NbtReadHelper.requireCompound(legacyOverrideTag, "mask");
                CollectionVisibilityMask legacyMask = new CollectionVisibilityMask();
                legacyMask.readFromNBT(legacyMaskTag);

                if (!legacyMask.hasSome()) {
                    continue;
                }

                CompoundTag migratedOverrideTag = new CompoundTag();
                migratedOverrideTag.putString("id", id.toString());
                migratedOverrideTag.put("visibility", writeSparseOverrideData(legacyData, legacyMask));
                migratedOverridesTagList.add(migratedOverrideTag);
            } catch (InvalidNbtFormatException e) {
                MapFrontiers.LOGGER.warn("Skipping invalid collection override at overrides[{}]: {}", i, e.getMessage());
            }
        }

        return migratedOverridesTagList;
    }

    private Pair<CollectionVisibilityData, CollectionVisibilityMask> readSparseOverrideData(CompoundTag dataTag) {
        CollectionVisibilityData data = new CollectionVisibilityData();
        CollectionVisibilityMask mask = new CollectionVisibilityMask();

        readSparseBooleanValue(dataTag, "visible", data::setVisible, mask::setVisible);
        readSparseIntValue(dataTag, "fullscreenZoom", data::setFullscreenZoom, mask::setFullscreenZoom);
        readSparseIntValue(dataTag, "minimapZoom", data::setMinimapZoom, mask::setMinimapZoom);
        readSparseIntValue(dataTag, "webmapZoom", data::setWebmapZoom, mask::setWebmapZoom);
        readSparseBooleanValue(dataTag, "fullscreenName", data::setFullscreenName, mask::setFullscreenName);
        readSparseBooleanValue(dataTag, "fullscreenOwner", data::setFullscreenOwner, mask::setFullscreenOwner);
        readSparseBooleanValue(dataTag, "fullscreenBanner", data::setFullscreenBanner, mask::setFullscreenBanner);
        readSparseBooleanValue(dataTag, "minimapName", data::setMinimapName, mask::setMinimapName);
        readSparseBooleanValue(dataTag, "minimapOwner", data::setMinimapOwner, mask::setMinimapOwner);
        readSparseBooleanValue(dataTag, "minimapBanner", data::setMinimapBanner, mask::setMinimapBanner);
        readSparseBooleanValue(dataTag, "webmapName", data::setWebmapName, mask::setWebmapName);
        readSparseBooleanValue(dataTag, "webmapOwner", data::setWebmapOwner, mask::setWebmapOwner);
        readSparseBooleanValue(dataTag, "webmapBanner", data::setWebmapBanner, mask::setWebmapBanner);

        return Pair.of(data, mask);
    }

    private CompoundTag writeSparseOverrideData(CollectionVisibilityData data, CollectionVisibilityMask mask) {
        CompoundTag dataTag = new CompoundTag();

        writeSparseBooleanValue(dataTag, "visible", data.isVisible(), mask.isVisible());
        writeSparseIntValue(dataTag, "fullscreenZoom", data.getFullscreenZoom(), mask.getFullscreenZoom());
        writeSparseIntValue(dataTag, "minimapZoom", data.getMinimapZoom(), mask.getMinimapZoom());
        writeSparseIntValue(dataTag, "webmapZoom", data.getWebmapZoom(), mask.getWebmapZoom());
        writeSparseBooleanValue(dataTag, "fullscreenName", data.getFullscreenName(), mask.getFullscreenName());
        writeSparseBooleanValue(dataTag, "fullscreenOwner", data.getFullscreenOwner(), mask.getFullscreenOwner());
        writeSparseBooleanValue(dataTag, "fullscreenBanner", data.getFullscreenBanner(), mask.getFullscreenBanner());
        writeSparseBooleanValue(dataTag, "minimapName", data.getMinimapName(), mask.getMinimapName());
        writeSparseBooleanValue(dataTag, "minimapOwner", data.getMinimapOwner(), mask.getMinimapOwner());
        writeSparseBooleanValue(dataTag, "minimapBanner", data.getMinimapBanner(), mask.getMinimapBanner());
        writeSparseBooleanValue(dataTag, "webmapName", data.getWebmapName(), mask.getWebmapName());
        writeSparseBooleanValue(dataTag, "webmapOwner", data.getWebmapOwner(), mask.getWebmapOwner());
        writeSparseBooleanValue(dataTag, "webmapBanner", data.getWebmapBanner(), mask.getWebmapBanner());

        return dataTag;
    }

    private void readSparseBooleanValue(CompoundTag dataTag, String key,
                                        java.util.function.Consumer<Boolean> dataSetter,
                                        java.util.function.Consumer<Boolean> maskSetter) {
        if (!dataTag.contains(key)) {
            return;
        }

        dataSetter.accept(dataTag.getBooleanOr(key, false));
        maskSetter.accept(true);
    }

    private void readSparseIntValue(CompoundTag dataTag, String key,
                                    java.util.function.IntConsumer dataSetter,
                                    java.util.function.Consumer<Boolean> maskSetter) {
        if (!dataTag.contains(key)) {
            return;
        }

        dataSetter.accept(dataTag.getIntOr(key, CollectionVisibilityData.COLLECTION_VIEW_DISABLED_ZOOM));
        maskSetter.accept(true);
    }

    private void writeSparseBooleanValue(CompoundTag dataTag, String key, boolean value, boolean masked) {
        if (!masked) {
            return;
        }

        dataTag.putBoolean(key, value);
    }

    private void writeSparseIntValue(CompoundTag dataTag, String key, int value, boolean masked) {
        if (!masked) {
            return;
        }

        dataTag.putInt(key, value);
    }

    private static void applyMaskedBoolean(boolean masked, boolean value, java.util.function.Consumer<Boolean> setter) {
        if (masked) {
            setter.accept(value);
        }
    }

    private static void applyMaskedInt(boolean masked, int value, java.util.function.IntConsumer setter) {
        if (masked) {
            setter.accept(value);
        }
    }
}
