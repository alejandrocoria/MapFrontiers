package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.territory.ClientMapFrontiersStorageHelper;
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

    private boolean readFromNBT(CompoundTag nbt) {
        boolean needBackup = false;
        try {
            int version = nbt.getIntOr("Version", 0);
            if (version == 0) {
                MapFrontiers.LOGGER.warn("Data version in {} not found, expected {}", FILENAME, MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            } else if (version > MapFrontiers.FRONTIER_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in {} higher than expected. The mod uses {}", FILENAME, MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            }

            ListTag overridesTagList = nbt.getListOrEmpty("overrides");
            for (int i = 0; i < overridesTagList.size(); ++i) {
                try {
                    CompoundTag overrideTag = NbtReadHelper.requireCompound(overridesTagList, i, "overrides");
                    UUID id = UUID.fromString(NbtReadHelper.requireString(overrideTag, "id"));

                    CompoundTag dataTag = NbtReadHelper.requireCompound(overrideTag, "data");
                    VisibilityData data = new VisibilityData();
                    data.readFromNBT(dataTag);

                    CompoundTag maskTag = NbtReadHelper.requireCompound(overrideTag, "mask");
                    VisibilityData mask = new VisibilityData(false);
                    mask.readFromNBT(maskTag);

                    if (mask.hasSome()) {
                        overrides.put(id, Pair.of(data, mask));
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
            CompoundTag overrideTag = new CompoundTag();
            overrideTag.putString("id", override.getKey().toString());

            CompoundTag dataTag = new CompoundTag();
            override.getValue().first().writeToNBT(dataTag);
            overrideTag.put("data", dataTag);

            CompoundTag maskTag = new CompoundTag();
            override.getValue().second().writeToNBT(maskTag);
            overrideTag.put("mask", maskTag);

            overridesTagList.add(overrideTag);
        }
        nbt.put("overrides", overridesTagList);

        nbt.putInt("Version", MapFrontiers.FRONTIER_DATA_VERSION);
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
}
