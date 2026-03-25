package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtFileHelper;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import games.alejandrocoria.mapfrontiers.platform.Services;
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
    private final Map<UUID, Pair<FrontierData.VisibilityData, FrontierData.VisibilityData>> overrides = new HashMap<>();
    private File ModDir;

    public FrontierLocalOverrides() {
        loadData();
    }

    public Pair<FrontierData.VisibilityData, FrontierData.VisibilityData> getVisibility(UUID id) {
        var override = overrides.get(id);
        if (override == null) {
            return Pair.of(new FrontierData.VisibilityData(), new FrontierData.VisibilityData(false));
        }
        return override;
    }

    public void setVisibility(UUID id, Pair<FrontierData.VisibilityData, FrontierData.VisibilityData> visibility) {
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
                MapFrontiers.LOGGER.warn("Data version in frontier_overrides not found, expected " + MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            } else if (version > MapFrontiers.FRONTIER_DATA_VERSION) {
                MapFrontiers.LOGGER.warn("Data version in frontier_overrides higher than expected. The mod uses " + MapFrontiers.FRONTIER_DATA_VERSION);
                needBackup = true;
            }

            ListTag overridesTagList = nbt.getListOrEmpty("overrides");
            for (int i = 0; i < overridesTagList.size(); ++i) {
                try {
                    CompoundTag overrideTag = NbtReadHelper.requireCompound(overridesTagList, i, "overrides");
                    UUID id = UUID.fromString(NbtReadHelper.requireString(overrideTag, "id"));

                    CompoundTag dataTag = NbtReadHelper.requireCompound(overrideTag, "data");
                    FrontierData.VisibilityData data = new FrontierData.VisibilityData();
                    data.readFromNBT(dataTag, version);

                    CompoundTag maskTag = NbtReadHelper.requireCompound(overrideTag, "mask");
                    FrontierData.VisibilityData mask = new FrontierData.VisibilityData(false);
                    mask.readFromNBT(maskTag, version);

                    if (mask.hasSome()) {
                        overrides.put(id, Pair.of(data, mask));
                    }
                } catch (InvalidNbtFormatException e) {
                    MapFrontiers.LOGGER.warn("Skipping invalid frontier override at overrides[{}]: {}", i, e.getMessage());
                    needBackup = true;
                }
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.warn("Failed to read frontier_overrides.dat: {}", e.getMessage());
            return true;
        }

        return needBackup;
    }

    private void writeToNBT(CompoundTag nbt) {
        ListTag overridesTagList = new ListTag();
        for (Map.Entry<UUID, Pair<FrontierData.VisibilityData, FrontierData.VisibilityData>> override : overrides.entrySet()) {
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
            File jmDir = Services.JOURNEYMAP.getJMWorldDir(Minecraft.getInstance());
            ModDir = new File(jmDir, "mapfrontier");
            //noinspection ResultOfMethodCallIgnored
            ModDir.mkdirs();

            CompoundTag nbtFrontiers = loadFile("frontier_overrides.dat");
            if (!nbtFrontiers.isEmpty()) {
                if (readFromNBT(nbtFrontiers)) {
                    NbtFileHelper.createBackup(ModDir, "frontier_overrides.dat");
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
            saveFile("frontier_overrides.dat", nbtFrontiers);
        }
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
